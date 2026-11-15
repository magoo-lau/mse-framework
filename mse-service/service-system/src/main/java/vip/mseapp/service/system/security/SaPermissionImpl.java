package vip.mseapp.service.system.security;

import cn.dev33.satoken.stp.StpInterface;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.system.service.SysUserService;

import java.util.List;

/**
 * Sa-Token 权限数据源：按 loginType 分流（platform 平台端 / tenant 租户端），结果经 Redis 缓存。
 *
 * <p>计算语义见 {@link AccessCodeCalculator}；业务微服务（未装配本模块）经 starter-bridge
 * 的 RPC 版数据源消费同一契约，缓存共用（{@link PermissionCache}），版本失效对两端同时生效。
 */
public class SaPermissionImpl implements StpInterface {

    private final SysUserService userService;
    private final AccessService accessService;
    private final PermissionCache permissionCache;

    public SaPermissionImpl(SysUserService userService, AccessService accessService,
                            PermissionCache permissionCache) {
        this.userService = userService;
        this.accessService = accessService;
        this.permissionCache = permissionCache;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!known(loginType)) {
            return List.of();
        }
        long[] ids = LoginIds.parse(loginId, loginType);
        return List.copyOf(permissionCache.getOrLoad(ids[0], ids[1], PermissionCache.KIND_PERMISSIONS,
                () -> AccessCodeCalculator.permissions(userService, accessService, loginType, loginId)));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!known(loginType)) {
            return List.of();
        }
        long[] ids = LoginIds.parse(loginId, loginType);
        return List.copyOf(permissionCache.getOrLoad(ids[0], ids[1], PermissionCache.KIND_ROLES,
                () -> AccessCodeCalculator.roles(accessService, loginType, loginId)));
    }

    private static boolean known(String loginType) {
        return MseConstants.LOGIN_TYPE_PLATFORM.equals(loginType)
                || MseConstants.LOGIN_TYPE_TENANT.equals(loginType);
    }

    /** loginId 解析：平台端 [0, userId]；租户端 [tenantId, userId]（loginId 形如 "{tenantId}-{userId}"） */
    private static final class LoginIds {

        private static long[] parse(Object loginId, String loginType) {
            if (MseConstants.LOGIN_TYPE_PLATFORM.equals(loginType)) {
                return new long[]{MseConstants.PLATFORM_TENANT_ID, Long.parseLong(loginId.toString())};
            }
            String[] parts = loginId.toString().split("-");
            return new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1])};
        }
    }
}
