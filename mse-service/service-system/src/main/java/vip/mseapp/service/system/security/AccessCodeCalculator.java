package vip.mseapp.service.system.security;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.service.SysUserService;

import java.util.Set;

/**
 * 权限码/角色码计算（登录端分流 + 超管语义），无缓存纯计算。
 *
 * <p>两个消费方共用同一语义：本地鉴权数据源 {@link SaPermissionImpl}（缓存于基座进程）
 * 与 RPC 契约实现 {@code RemoteAccessServiceImpl}（缓存于业务微服务进程，见 starter-bridge）。
 */
public final class AccessCodeCalculator {

    private AccessCodeCalculator() {
    }

    /**
     * 计算用户权限码集合。
     *
     * <p>超管语义：平台超管返回通配 *（平台无套餐约束）；租户管理员返回「套餐域内全量权限码」——
     * 而非通配 *，保证套餐裁剪对租户管理员同样生效。
     */
    public static Set<String> permissions(SysUserService userService, AccessService accessService,
                                          String loginType, Object loginId) {
        if (MseConstants.LOGIN_TYPE_PLATFORM.equals(loginType)) {
            Long userId = Long.parseLong(loginId.toString());
            return TenantContext.runAs(MseConstants.PLATFORM_TENANT_ID, () -> {
                SysUser user = userService.getById(userId);
                if (user == null) {
                    return Set.<String>of();
                }
                if (Boolean.TRUE.equals(user.getSuperAdmin())) {
                    return Set.of("*");
                }
                return accessService.getPermissionCodesForUser(
                        true, MseConstants.PLATFORM_TENANT_ID, userId, false);
            });
        }
        if (MseConstants.LOGIN_TYPE_TENANT.equals(loginType)) {
            String[] parts = loginId.toString().split("-");
            Long tenantId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            return TenantContext.runAs(tenantId, () -> {
                SysUser user = userService.getById(userId);
                if (user == null) {
                    return Set.<String>of();
                }
                return accessService.getPermissionCodesForUser(
                        false, tenantId, userId, Boolean.TRUE.equals(user.getSuperAdmin()));
            });
        }
        return Set.of();
    }

    /** 计算用户角色编码集合（需租户上下文语义同上） */
    public static Set<String> roles(AccessService accessService, String loginType, Object loginId) {
        if (MseConstants.LOGIN_TYPE_PLATFORM.equals(loginType)) {
            Long userId = Long.parseLong(loginId.toString());
            return TenantContext.runAs(MseConstants.PLATFORM_TENANT_ID, () ->
                    accessService.getRoleCodesForUser(userId));
        }
        if (MseConstants.LOGIN_TYPE_TENANT.equals(loginType)) {
            String[] parts = loginId.toString().split("-");
            return TenantContext.runAs(Long.parseLong(parts[0]), () ->
                    accessService.getRoleCodesForUser(Long.parseLong(parts[1])));
        }
        return Set.of();
    }
}
