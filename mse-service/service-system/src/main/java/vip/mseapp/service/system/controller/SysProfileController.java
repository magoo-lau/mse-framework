package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.api.system.RemoteAuthService.TotpEnrollment;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.security.AccessService;
import vip.mseapp.service.system.service.SysMenuService;
import vip.mseapp.service.system.service.SysUserService;
import vip.mseapp.service.system.service.TwoFactorService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.mseapp.service.system.service.AvatarStorage;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 个人中心：当前登录者信息 + 改密 + 头像上传。
 *
 * <p>/me 是前端动态路由与按钮权限的数据源（用户 + 菜单树 + 权限码集合 + 角色集合），
 * 双端各一组端点（Sa-Token 注解按 loginType 精确区分）。头像本地素材读取
 * /system/profile/asset/{name} 免登录（img 标签不携带 token，微服务形态经网关 ignore-urls 放行）。
 */
@Validated
@RestController
@RequestMapping("/system/profile")
public class SysProfileController {

    private final SysUserService userService;
    private final AccessService accessService;
    private final AvatarStorage avatarStorage;
    private final TwoFactorService twoFactorService;

    /** 租户名称归 service-tenant 持有，经 RPC 契约查询（单体下本地直调） */
    @RpcReference
    private RemoteTenantService remoteTenantService;

    public SysProfileController(SysUserService userService, AccessService accessService,
                                AvatarStorage avatarStorage, TwoFactorService twoFactorService) {
        this.userService = userService;
        this.accessService = accessService;
        this.avatarStorage = avatarStorage;
        this.twoFactorService = twoFactorService;
    }

    /** totpMode=当前生效双因子模式（本租户→平台域→默认 off），前端据此显隐头像菜单绑定入口；
     * appsRegistered=是否登记过启用子应用，前端据此区分 microApps 空列表的「不可见」与「未登记」双语义 */
    public record ProfileVO(SysUser user, Set<String> roles, Set<String> permissions,
                            List<SysMenuService.MenuTreeNode> menus,
                            List<AccessService.MicroAppVO> microApps, boolean appsRegistered,
                            String tenantName, String totpMode) {
    }

    @SaCheckPermission(value = "system:profile:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/me")
    public Result<ProfileVO> me(@RequestParam(required = false) Integer client) {
        return Result.ok(buildProfile(resolveClient(client)));
    }

    @SaCheckPermission(value = "system:profile:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/me")
    public Result<ProfileVO> platformMe(@RequestParam(required = false) Integer client) {
        return Result.ok(buildProfile(resolveClient(client)));
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated PasswordChangeRequest request) {
        return Result.ok(changePasswordInternal(request));
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform/password")
    public Result<Void> platformChangePassword(@RequestBody @Validated PasswordChangeRequest request) {
        return Result.ok(changePasswordInternal(request));
    }

    /** 头像上传（双端共用，走 mse-fs 公开素材通道或本地磁盘兜底），返回可写入 avatar 的站内地址 */
    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(updateAvatarInternal(file));
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/avatar")
    public Result<String> platformUploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(updateAvatarInternal(file));
    }

    /** 头像读取（本地兜底实现专用；mse-fs 模式头像走 /fs/asset/**，不经此端点） */
    @GetMapping("/asset/{name}")
    public void asset(@PathVariable String name, HttpServletResponse response) throws IOException {
        avatarStorage.write(name, response);
    }

    // -------------------- TOTP 双因子（个人自助绑定/解绑，复用 system:profile:update 权限码） --------------------

    /** 查询绑定状态（仅布尔，密钥本体不出本服务） */
    @SaCheckPermission(value = "system:profile:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/totp")
    public Result<TotpStatus> totpStatus() {
        return Result.ok(totpStatusInternal());
    }

    @SaCheckPermission(value = "system:profile:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/totp")
    public Result<TotpStatus> platformTotpStatus() {
        return Result.ok(totpStatusInternal());
    }

    /** 生成绑定挑战（二维码 + 手输密钥；密钥暂存 Redis，验证通过才落库） */
    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/totp/setup")
    public Result<TotpEnrollment> totpSetup() {
        return Result.ok(totpSetupInternal());
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/totp/setup")
    public Result<TotpEnrollment> platformTotpSetup() {
        return Result.ok(totpSetupInternal());
    }

    /** 确认绑定：输入验证器 App 动态码校验通过后生效 */
    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/totp/enable")
    public Result<Void> totpEnable(@RequestBody @Validated TotpCodeRequest request) {
        return Result.ok(totpEnableInternal(request));
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/totp/enable")
    public Result<Void> platformTotpEnable(@RequestBody @Validated TotpCodeRequest request) {
        return Result.ok(totpEnableInternal(request));
    }

    /** 解绑：校验当前动态码后清除（丢失手机时由管理员经「用户管理 → 重置双因子」处理） */
    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/totp/disable")
    public Result<Void> totpDisable(@RequestBody @Validated TotpCodeRequest request) {
        return Result.ok(totpDisableInternal(request));
    }

    @SaCheckPermission(value = "system:profile:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/totp/disable")
    public Result<Void> platformTotpDisable(@RequestBody @Validated TotpCodeRequest request) {
        return Result.ok(totpDisableInternal(request));
    }

    private TotpStatus totpStatusInternal() {
        return new TotpStatus(twoFactorService.isTotpBound(TenantContext.getRequiredTenantId(), UserContext.getUserId()));
    }

    private TotpEnrollment totpSetupInternal() {
        return twoFactorService.createPendingTotp(TenantContext.getRequiredTenantId(), UserContext.getUserId());
    }

    private Void totpEnableInternal(TotpCodeRequest request) {
        boolean bound = twoFactorService.confirmPendingTotp(
                TenantContext.getRequiredTenantId(), UserContext.getUserId(), request.code());
        if (!bound) {
            throw new ServiceException(400, "动态验证码错误");
        }
        return null;
    }

    private Void totpDisableInternal(TotpCodeRequest request) {
        twoFactorService.unbindTotp(TenantContext.getRequiredTenantId(), UserContext.getUserId(), request.code());
        return null;
    }

    private String updateAvatarInternal(MultipartFile file) {
        String url = avatarStorage.upload(file);
        userService.updateAvatar(UserContext.getUserId(), url);
        return url;
    }

    private ProfileVO buildProfile(int client) {
        Long userId = UserContext.getUserId();
        boolean platform = Boolean.TRUE.equals(UserContext.isPlatform());
        Long tenantId = TenantContext.getRequiredTenantId();
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        user.setPassword(null);
        boolean superAdmin = Boolean.TRUE.equals(user.getSuperAdmin());
        return new ProfileVO(
                user,
                accessService.getRoleCodesForUser(userId),
                // 超管返回域内全量权限码（而非 *）：前端按钮判定与后端鉴权语义一致
                accessService.getPermissionCodesForUser(platform, tenantId, userId, superAdmin),
                accessService.getMenusForUser(platform, tenantId, userId, superAdmin, client),
                accessService.getMicroAppsForUser(platform, tenantId, client),
                accessService.hasRegisteredApp(),
                resolveTenantName(platform, tenantId),
                twoFactorService.effectiveTotpMode(tenantId));
    }

    /** 客户端口径归一：缺省 PC（存量 PC 基座不传参零改动），非法值回落 PC */
    private int resolveClient(Integer client) {
        return client != null && client == SysMenu.CLIENT_MOBILE ? SysMenu.CLIENT_MOBILE : SysMenu.CLIENT_PC;
    }

    /** 顶栏租户标识显示名称用；平台端无租户归属返回 null，租户已删除时回退 null（前端退回显示编号） */
    private String resolveTenantName(boolean platform, Long tenantId) {
        if (platform) {
            return null;
        }
        TenantDTO tenant = remoteTenantService.getTenantById(tenantId);
        return tenant == null ? null : tenant.getName();
    }

    private Void changePasswordInternal(PasswordChangeRequest request) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        if (user == null || !MsePasswords.matches(request.oldPassword(), user.getPassword())) {
            throw new ServiceException(1102, "原密码不正确");
        }
        userService.resetPassword(userId, MsePasswords.encode(request.newPassword()));
        return null;
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "原密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空") @jakarta.validation.constraints.Size(min = 6, max = 32) String newPassword) {
    }

    /** TOTP 绑定状态（仅布尔，不含密钥） */
    public record TotpStatus(boolean bound) {
    }

    /** TOTP 动态码请求（绑定确认/解绑共用） */
    public record TotpCodeRequest(
            @NotBlank(message = "动态验证码不能为空") @jakarta.validation.constraints.Size(max = 10) String code) {
    }
}
