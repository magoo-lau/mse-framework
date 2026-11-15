package vip.mseapp.redis;

/**
 * 租户控制面缓存 key 约定（租户×服务路由快照 + 租户行快照，{@link TenantCache} 使用）。
 *
 * <p>失效策略与 {@link MsePermCacheKeys} 同构：版本号 + TTL 双保险——
 * <ul>
 *   <li>租户级版本：该租户的路由配置 / 租户行变更时自增（同租户的路由条目与租户行条目一并失效）；</li>
 *   <li>全局级版本：租户数据源 CRUD 时自增（shared/dedicated 判定影响所有租户的路由条目）；</li>
 *   <li>TTL 兜底，防止遗漏的失效路径长期残留旧数据。</li>
 * </ul>
 * 版本号方案避免 KEYS 模式扫描；旧版本 key 随 TTL 自然过期。
 */
public final class MseTenantCacheKeys {

    private MseTenantCacheKeys() {
    }

    /** 全局级版本号（租户数据源 CRUD 时 INCR） */
    public static String globalVer() {
        return "mse:tenant:global:ver";
    }

    /** 租户级版本号（该租户路由配置/租户行变更时 INCR） */
    public static String tenantVer(long tenantId) {
        return "t:" + tenantId + ":tenant:ver";
    }

    /** 租户×服务路由快照条目（双版本：全局.租户） */
    public static String route(long tenantId, String serviceName, long globalVer, long tenantVer) {
        return "t:" + tenantId + ":route:" + serviceName + ":v" + globalVer + "." + tenantVer;
    }

    /** 租户行快照条目（仅受租户级版本约束——租户行不受数据源 CRUD 影响） */
    public static String tenantInfo(long tenantId, long tenantVer) {
        return "t:" + tenantId + ":info:v" + tenantVer;
    }
}
