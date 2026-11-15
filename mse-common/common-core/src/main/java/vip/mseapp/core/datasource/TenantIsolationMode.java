package vip.mseapp.core.datasource;

/**
 * 租户×服务级隔离模式（sys_tenant_service_config.isolation_mode）。
 */
public enum TenantIsolationMode {

    /** 独立库：@TenantDatasource 实体路由到绑定数据源，不做字段/分表改写 */
    INDEPENDENT_DB,

    /** 共享库分表：按租户后缀改写表名（order → order_t001） */
    SHARED_TABLE_SPLIT,

    /** 共享库字段隔离：租户插件追加 tenant_id 条件（现状默认行为） */
    SHARED_FIELD
}
