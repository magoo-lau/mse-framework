package vip.mseapp.rpc.bridge;

import cn.dev33.satoken.stp.StpInterface;
import vip.mseapp.api.system.RemoteAccessService;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.redis.PermissionCache;

import java.util.HashSet;
import java.util.List;

/**
 * Sa-Token 鉴权数据源的 RPC 实现（消费 mse-system 的 {@link RemoteAccessService} 契约）。
 *
 * <p>面向未装配 service-system 的业务微服务：登录态由网关校验（共享 Redis），
 * 细粒度权限码在本服务注解校验，数据经契约向 mse-system 拉取并按「版本号 + TTL」
 * 缓存（{@link PermissionCache} 与基座共用同一 key 体系——版本失效对两端同时生效）。
 * 基座本地实现（service-system）在 classpath 时本类不装配。
 */
public class RpcSaPermission implements StpInterface {

    @RpcReference
    private RemoteAccessService remoteAccessService;

    private final PermissionCache permissionCache;

    public RpcSaPermission(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!known(loginType)) {
            return List.of();
        }
        long[] ids = LoginIds.parse(loginId, loginType);
        return List.copyOf(permissionCache.getOrLoad(ids[0], ids[1], PermissionCache.KIND_PERMISSIONS,
                () -> new HashSet<>(remoteAccessService.getPermissionList(loginType, loginId.toString()))));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!known(loginType)) {
            return List.of();
        }
        long[] ids = LoginIds.parse(loginId, loginType);
        return List.copyOf(permissionCache.getOrLoad(ids[0], ids[1], PermissionCache.KIND_ROLES,
                () -> new HashSet<>(remoteAccessService.getRoleList(loginType, loginId.toString()))));
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
