package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.excel.UserExcelRow;
import vip.mseapp.service.system.service.SysUserService;
import vip.mseapp.service.system.service.TwoFactorService;
import vip.mseapp.web.log.OperLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户管理：/system/user/** 租户内操作；/system/user/platform/** 平台端操作（tenant_id=0 域）。
 * 租户内分页叠加数据范围过滤（部门/本人）。导入/导出仅租户端提供。
 */
@Validated
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService userService;
    private final TwoFactorService twoFactorService;

    public SysUserController(SysUserService userService, TwoFactorService twoFactorService) {
        this.userService = userService;
        this.twoFactorService = twoFactorService;
    }

    // -------------------- 租户内 --------------------

    @SaCheckPermission(value = "system:user:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String username,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Long deptId) {
        return Result.ok(userService.pageUsers(pageNum, pageSize, username, status, deptId));
    }

    @SaCheckPermission(value = "system:user:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        // 缺行（含跨租户 id 探测——独立库路由下落本库未命中）统一 404，不回 code=0/data=null
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return Result.ok(user);
    }

    @OperLog(module = "system:user", type = "新增")
    @SaCheckPermission(value = "system:user:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated UserCreateRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(MsePasswords.encode(request.password()));
        user.setNickname(request.nickname());
        user.setMobile(request.mobile());
        user.setEmail(request.email());
        user.setDeptId(request.deptId());
        user.setSuperAdmin(Boolean.FALSE);
        user.setStatus(0);
        Long userId = userService.createUser(user);
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            userService.assignRoles(userId, request.roleIds());
        }
        userService.assignPosts(userId, request.postIds());
        return Result.ok(userId);
    }

    @OperLog(module = "system:user", type = "修改")
    @SaCheckPermission(value = "system:user:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated UserUpdateRequest request) {
        SysUser user = new SysUser();
        user.setId(request.id());
        user.setNickname(request.nickname());
        user.setMobile(request.mobile());
        user.setEmail(request.email());
        user.setStatus(request.status());
        user.setDeptId(request.deptId());
        userService.updateUser(user);
        if (request.roleIds() != null) {
            userService.assignRoles(request.id(), request.roleIds());
        }
        if (request.postIds() != null) {
            userService.assignPosts(request.id(), request.postIds());
        }
        return Result.ok();
    }

    @OperLog(module = "system:user", type = "重置密码")
    @SaCheckPermission(value = "system:user:reset-password", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody @Validated PasswordResetRequest request) {
        userService.resetPassword(id, MsePasswords.encode(request.password()));
        return Result.ok();
    }

    /** 重置双因子：清除用户 TOTP 绑定（丢失验证器时的管理端恢复手段，用户下次登录可重新绑定） */
    @OperLog(module = "system:user", type = "重置双因子")
    @SaCheckPermission(value = "system:user:reset-totp", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/{id}/totp-reset")
    public Result<Void> resetTotp(@PathVariable Long id) {
        twoFactorService.resetTotp(TenantContext.getRequiredTenantId(), id);
        return Result.ok();
    }

    @OperLog(module = "system:user", type = "删除")
    @SaCheckPermission(value = "system:user:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @SaCheckPermission(value = "system:user:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}/role-ids")
    public Result<List<Long>> roleIds(@PathVariable Long id) {
        return Result.ok(userService.getUserRoleIds(id));
    }

    @SaCheckPermission(value = "system:user:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}/post-ids")
    public Result<List<Long>> postIds(@PathVariable Long id) {
        return Result.ok(userService.getUserPostIds(id));
    }

    // -------------------- 导入导出（租户端） --------------------

    @OperLog(module = "system:user", type = "导出")
    @SaCheckPermission(value = "system:user:export", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer status) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        List<UserExcelRow> rows = userService.listUsersForExport(username, status).stream()
                .map(SysUserController::toExcelRow)
                .toList();
        // 密码列仅导入模板需要（导入可选填，缺省取 sys.user.initPassword），导出按类 Javadoc 语义排除
        EasyExcel.write(response.getOutputStream(), UserExcelRow.class)
                .excludeColumnFieldNames(List.of("password")).sheet("用户数据").doWrite(rows);
    }

    @OperLog(module = "system:user", type = "导入")
    @SaCheckPermission(value = "system:user:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/import")
    public Result<SysUserService.UserImportResult> importUsers(@RequestParam("file") MultipartFile file,
                                                               @RequestParam(defaultValue = "false") boolean updateSupport) throws IOException {
        List<UserExcelRow> rows = EasyExcel.read(file.getInputStream()).head(UserExcelRow.class)
                .sheet().doReadSync();
        List<SysUserService.UserImportRow> importRows = rows.stream()
                .map(row -> new SysUserService.UserImportRow(row.getUsername(), row.getNickname(),
                        row.getMobile(), row.getEmail(), row.getDeptId(), row.getStatus(), row.getPassword()))
                .toList();
        return Result.ok(userService.importUsers(importRows, updateSupport));
    }

    /** 导入模板（空数据导出，含表头与示例行说明） */
    @SaCheckPermission(value = "system:user:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), UserExcelRow.class).sheet("用户数据").doWrite(List.of());
    }

    static UserExcelRow toExcelRow(SysUser user) {
        UserExcelRow row = new UserExcelRow();
        row.setId(user.getId());
        row.setUsername(user.getUsername());
        row.setNickname(user.getNickname());
        row.setDeptId(user.getDeptId());
        row.setMobile(user.getMobile());
        row.setEmail(user.getEmail());
        row.setStatus(user.getStatus());
        return row;
    }

    // -------------------- 平台端（tenant_id=0 域） --------------------

    @SaCheckPermission(value = "system:user:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/page")
    public Result<PageResult<SysUser>> platformPage(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String username,
                                                    @RequestParam(required = false) Integer status) {
        return Result.ok(userService.pageUsersPlatform(pageNum, pageSize, username, status));
    }

    @OperLog(module = "system:user", type = "新增")
    @SaCheckPermission(value = "system:user:platform-create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform")
    public Result<Long> platformCreate(@RequestBody @Validated PlatformUserCreateRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(MsePasswords.encode(request.password()));
        user.setNickname(request.nickname());
        user.setDeptId(request.deptId());
        user.setSuperAdmin(Boolean.FALSE);
        user.setStatus(0);
        Long userId = userService.createUser(user);
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            userService.assignRoles(userId, request.roleIds());
        }
        return Result.ok(userId);
    }

    @OperLog(module = "system:user", type = "修改")
    @SaCheckPermission(value = "system:user:platform-update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform")
    public Result<Void> platformUpdate(@RequestBody @Validated UserUpdateRequest request) {
        SysUser user = new SysUser();
        user.setId(request.id());
        user.setNickname(request.nickname());
        user.setMobile(request.mobile());
        user.setEmail(request.email());
        user.setStatus(request.status());
        user.setDeptId(request.deptId());
        userService.updateUser(user);
        if (request.roleIds() != null) {
            userService.assignRoles(request.id(), request.roleIds());
        }
        return Result.ok();
    }

    @OperLog(module = "system:user", type = "删除")
    @SaCheckPermission(value = "system:user:platform-delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/platform/{id}")
    public Result<Void> platformDelete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @SaCheckPermission(value = "system:user:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/{id}/role-ids")
    public Result<List<Long>> platformRoleIds(@PathVariable Long id) {
        return Result.ok(userService.getUserRoleIds(id));
    }

    /** 平台端重置平台用户双因子 */
    @OperLog(module = "system:user", type = "重置双因子")
    @SaCheckPermission(value = "system:user:platform-reset-totp", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform/{id}/totp-reset")
    public Result<Void> platformResetTotp(@PathVariable Long id) {
        twoFactorService.resetTotp(MseConstants.PLATFORM_TENANT_ID, id);
        return Result.ok();
    }

    // -------------------- 请求体 --------------------

    public record UserCreateRequest(
            @NotBlank(message = "用户名不能为空") @Size(max = 30) String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 32) String password,
            String nickname,
            String mobile,
            String email,
            Long deptId,
            List<Long> roleIds,
            List<Long> postIds) {
    }

    public record UserUpdateRequest(
            @NotNull(message = "用户编号不能为空") Long id,
            String nickname,
            String mobile,
            String email,
            Integer status,
            Long deptId,
            List<Long> roleIds,
            List<Long> postIds) {
    }

    public record PlatformUserCreateRequest(
            @NotBlank(message = "用户名不能为空") @Size(max = 30) String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 32) String password,
            String nickname,
            Long deptId,
            List<Long> roleIds) {
    }

    public record PasswordResetRequest(
            @NotBlank @Size(min = 6, max = 32, message = "密码长度须为 6-32 位") String password) {
    }
}
