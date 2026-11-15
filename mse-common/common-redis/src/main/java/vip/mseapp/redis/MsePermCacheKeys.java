package vip.mseapp.redis;

/**
 * 权限模型缓存 key 约定（Sa-Token 鉴权数据源的 Redis 缓存）。
 *
 * <p>失效策略采用「版本号 + TTL」双保险：
 * <ul>
 *   <li>租户级版本：角色授权 / 用户角色变更时自增，仅影响该租户的全部用户；</li>
 *   <li>全局级版本：菜单 / 权限点 / 套餐定义变更时自增，影响所有租户的全部用户；</li>
 *   <li>TTL 兜底（默认 30 分钟），防止遗漏的失效路径长期残留旧数据。</li>
 * </ul>
 * 版本号方案避免 KEYS 模式扫描；旧版本 key 随 TTL 自然过期。
 */
public final class MsePermCacheKeys {

    private MsePermCacheKeys() {
    }

    /** 全局级版本号（全局表定义变更时 INCR） */
    public static String globalVer() {
        return "mse:perm:global:ver";
    }

    /** 租户级版本号（该租户内授权关系变更时 INCR） */
    public static String tenantVer(long tenantId) {
        return "t:" + tenantId + ":perm:ver";
    }

    /** 用户权限集合（code 集合）或角色集合（role code 集合） */
    public static String userPerm(long tenantId, long userId, String kind, long globalVer, long tenantVer) {
        return "t:" + tenantId + ":perm:" + kind + ":" + userId + ":v" + globalVer + "." + tenantVer;
    }
}
