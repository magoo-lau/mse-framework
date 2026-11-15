package vip.mseapp.mq;

import java.io.Serializable;

/**
 * 租户事件消息（控制面 → 数据面的标准载荷）。
 *
 * <p>租户归属随消息体传递（userProperty 头仅作观测），消费端用
 * {@code TenantContext.runAs(event.tenantId(), () -> ...)} 恢复租户语义后处理，
 * 保证异步链路的 SQL 隔离与数据源路由与同步链路一致。
 *
 * @param tenantId  租户编号
 * @param eventType 事件类型（见 {@link Type}）
 * @param payload   业务数据（JSON 序列化）
 */
public record TenantEvent(Long tenantId, String eventType, String payload) implements Serializable {

    /** 事件主题（控制面 → 数据面） */
    public static final String TOPIC = "mse-tenant-events";

    /** 事件类型常量（控制面/数据面按此解耦） */
    public interface Type {
        /** 开通租户：数据面初始化每租户种子数据、快照自举 */
        String CREATED = "tenant.created";
        /** 恢复启用 */
        String ACTIVATED = "tenant.activated";
        /** 冻结停用 */
        String SUSPENDED = "tenant.suspended";
        /** 功能开关/套餐变更：刷新本地快照缓存 */
        String FEATURE_CHANGED = "tenant.feature.changed";
        /** 配额变更：刷新本地配额缓存 */
        String QUOTA_CHANGED = "tenant.quota.changed";
        /** 手动触发：为指定租户补跑 pending 每租户脚本 */
        String DATA_UPGRADE = "tenant.data-upgrade";
        /** 服务路由/表级策略/数据源绑定变更：数据面失效租户快照（tenantId=null 表示全局变更，全量失效） */
        String ROUTE_CHANGED = "tenant.route.changed";
        /** 注销租户：清理租户数据与数据面缓存键 */
        String DELETED = "tenant.deleted";
    }

    public static TenantEvent of(Long tenantId, String eventType, Object payload) {
        return new TenantEvent(tenantId, eventType, String.valueOf(payload));
    }
}
