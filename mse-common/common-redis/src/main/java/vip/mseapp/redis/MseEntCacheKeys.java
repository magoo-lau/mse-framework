package vip.mseapp.redis;

/** 租户权益缓存 key 约定（EntitlementCache：版本号 + TTL 双保险，范式同 MsePermCacheKeys） */
public final class MseEntCacheKeys {

    /** 全局版本（权益项/套餐项/附加项定义变更时 INCR） */
    public static final String GLOBAL_VER = "mse:ent:global:ver";

    private MseEntCacheKeys() {
    }

    /** 租户级版本（订购附加项/覆盖/换绑套餐变更时 INCR） */
    public static String tenantVer(long tenantId) {
        return "t:" + tenantId + ":ent:ver";
    }

    /** 权益条目（值=EntitlementDTO，携带双版本随失效自然淘汰） */
    public static String entitlement(long tenantId, long globalVer, long tenantVer) {
        return "t:" + tenantId + ":ent:" + globalVer + "." + tenantVer;
    }

    public static String globalVer() {
        return GLOBAL_VER;
    }
}
