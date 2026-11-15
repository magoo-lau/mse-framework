package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.service.SysOnlineService;
import vip.mseapp.web.log.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线会话管理：/system/online/** 租户端（仅本租户账号的会话）；/system/online/platform/** 平台端（世界 0 视角，仅平台账号体系自身的会话）。
 * 每个世界只能看/管自己世界的会话，平台不跨看、不跨管租户会话。
 *
 * <p>列表与下线均以不可逆会话标识（SHA-256 摘要）定位，原始 token 不出服务端。
 */
@Validated
@RestController
@RequestMapping("/system/online")
public class SysOnlineController {

    private final SysOnlineService onlineService;

    public SysOnlineController(SysOnlineService onlineService) {
        this.onlineService = onlineService;
    }

    // -------------------- 租户端（系统管理-在线用户，仅本租户） --------------------

    @SaCheckPermission(value = "system:online:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysOnlineService.OnlineSession>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(onlineService.pageSessions(pageNum, pageSize, TenantContext.getRequiredTenantId(), keyword));
    }

    @OperLog(module = "system:online", type = "强制下线")
    @SaCheckPermission(value = "system:online:kick", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/kick")
    public Result<Void> kick(@RequestBody @Validated KickSessionRequest request) {
        onlineService.kickSession(TenantContext.getRequiredTenantId(),
                new SysOnlineService.KickCommand(request.loginType(), request.tenantId(), request.userId(), request.sessionId()));
        return Result.ok();
    }

    @OperLog(module = "system:online", type = "强制下线")
    @SaCheckPermission(value = "system:online:kick", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/kick-user")
    public Result<Void> kickUser(@RequestBody @Validated KickUserRequest request) {
        onlineService.kickUser(TenantContext.getRequiredTenantId(),
                new SysOnlineService.KickCommand(request.loginType(), request.tenantId(), request.userId(), null));
        return Result.ok();
    }

    // -------------------- 平台端（平台管理-在线用户，世界 0，仅平台账号体系会话） --------------------

    @SaCheckPermission(value = "system:online:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/page")
    public Result<PageResult<SysOnlineService.OnlineSession>> platformPage(@RequestParam(defaultValue = "1") long pageNum,
                                                                           @RequestParam(defaultValue = "10") long pageSize,
                                                                           @RequestParam(required = false) String keyword) {
        return Result.ok(onlineService.pageSessions(pageNum, pageSize, MseConstants.PLATFORM_TENANT_ID, keyword));
    }

    @OperLog(module = "system:online", type = "强制下线")
    @SaCheckPermission(value = "system:online:platform-kick", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/kick")
    public Result<Void> platformKick(@RequestBody @Validated KickSessionRequest request) {
        onlineService.kickSession(MseConstants.PLATFORM_TENANT_ID,
                new SysOnlineService.KickCommand(request.loginType(), request.tenantId(), request.userId(), request.sessionId()));
        return Result.ok();
    }

    @OperLog(module = "system:online", type = "强制下线")
    @SaCheckPermission(value = "system:online:platform-kick", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform/kick-user")
    public Result<Void> platformKickUser(@RequestBody @Validated KickUserRequest request) {
        onlineService.kickUser(MseConstants.PLATFORM_TENANT_ID,
                new SysOnlineService.KickCommand(request.loginType(), request.tenantId(), request.userId(), null));
        return Result.ok();
    }

    // -------------------- 请求体 --------------------

    public record KickSessionRequest(
            @NotBlank(message = "账号体系不能为空") String loginType,
            @NotNull(message = "租户编号不能为空") Long tenantId,
            @NotNull(message = "用户编号不能为空") Long userId,
            @NotBlank(message = "会话标识不能为空") String sessionId) {
    }

    public record KickUserRequest(
            @NotBlank(message = "账号体系不能为空") String loginType,
            @NotNull(message = "租户编号不能为空") Long tenantId,
            @NotNull(message = "用户编号不能为空") Long userId) {
    }
}
