package vip.mseapp.mybatis.data;

/**
 * 平台控制面表实体基类（标记）：sys_tenant / sys_tenant_package / sys_tenant_datasource /
 * sys_tenant_service_config / sys_tenant_service_table_policy / sys_domain_config 等
 * 「跨租户元数据」——归属平台控制面库（mse-platform 主库；单体为 platform 数据源）。
 *
 * <p>继承 {@link GlobalBaseEntity}（无 tenant_id 语义的字段隔离豁免照旧），
 * 额外获得数据源路由：注册了 platform 数据源的环境（单体）自动切平台库，
 * 未注册环境（mse-platform 微服务自身 primary 即平台库）留在主库，行为自然正确。
 */
public abstract class PlatformBaseEntity extends GlobalBaseEntity {
}
