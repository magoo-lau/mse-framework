package vip.mseapp.core.datasource;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 租户×服务路由快照（由 TenantRouteResolver 组装，30s 缓存）。
 *
 * <p>解析顺序：sys_tenant_service_config(tenant,service) → sys_tenant.ds_key(全局默认，按数据源
 * 库模式区分语义：shared = SHARED_FIELD + 共享池路由，dedicated = 独立库) → 无配置 = SHARED_FIELD
 * + 缺省共享库（登记表 is_default 行的 dsKey，显式路由；无登记行的纯独立库部署须在解析侧快速失败）。
 */
public class TenantRoute implements Serializable {

    private final Long tenantId;

    private final String serviceName;

    /** 服务级隔离模式，永不为 null（缺省 SHARED_FIELD） */
    private final TenantIsolationMode mode;

    /** 路由数据源 key；null/空 = 不路由（落本服务业务主库） */
    private final String dsKey;

    /** 服务级分表后缀（SHARED_TABLE_SPLIT 缺省值） */
    private final String tableSuffix;

    /** 字段隔离列名（服务级，缺省 tenant_id） */
    private final String tenantColumn;

    /** 表级策略（逻辑表名 → 策略），不可变 */
    private final Map<String, TablePolicy> tablePolicies;

    public TenantRoute(Long tenantId, String serviceName, TenantIsolationMode mode, String dsKey,
                       String tableSuffix, String tenantColumn, Map<String, TablePolicy> tablePolicies) {
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.mode = mode == null ? TenantIsolationMode.SHARED_FIELD : mode;
        this.dsKey = dsKey == null || dsKey.isBlank() ? null : dsKey;
        this.tableSuffix = tableSuffix;
        this.tenantColumn = tenantColumn == null || tenantColumn.isBlank() ? "tenant_id" : tenantColumn;
        this.tablePolicies = tablePolicies == null ? Collections.emptyMap() : Map.copyOf(tablePolicies);
    }

    /**
     * 缺省共享库快照（字段隔离）。
     *
     * @param dsKey 内置共享库键（每服务自declare：默认=本服务应用名、显式 '*'=通用）；
     *              <b>null=落本服务内置共享库</b>（0.1.0 v2 恢复「null=本服务主库」语义的显式化：
     *              平台侧未绑定租户/无服务配置的兜底快照，由各服务拦截器本地解析内置键执行）
     */
    public static TenantRoute sharedDefault(Long tenantId, String serviceName, String dsKey) {
        return new TenantRoute(tenantId, serviceName, TenantIsolationMode.SHARED_FIELD, dsKey, null, "tenant_id", null);
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public TenantIsolationMode getMode() {
        return mode;
    }

    public String getDsKey() {
        return dsKey;
    }

    /** 是否路由数据源：独立库，或共享模式显式指定了共享池 */
    public boolean routeDatasource() {
        return dsKey != null;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public Map<String, TablePolicy> getTablePolicies() {
        return tablePolicies;
    }

    /** 独立库模式（该租户全量表在独立库，不做字段/分表改写） */
    public boolean independentDb() {
        return mode == TenantIsolationMode.INDEPENDENT_DB;
    }

    /** 表级生效模式：表级策略优先，否则由服务级模式推导（独立库场景不调用） */
    public TablePolicy.Mode effectiveMode(String logicalTable) {
        TablePolicy policy = tablePolicies.get(logicalTable);
        if (policy != null) {
            return policy.getMode();
        }
        return mode == TenantIsolationMode.SHARED_TABLE_SPLIT ? TablePolicy.Mode.SPLIT : TablePolicy.Mode.FIELD;
    }

    /** 表级生效分表后缀：表级策略优先，否则服务级缺省后缀 */
    public String effectiveSuffix(String logicalTable) {
        TablePolicy policy = tablePolicies.get(logicalTable);
        if (policy != null && policy.getTableSuffix() != null && !policy.getTableSuffix().isBlank()) {
            return policy.getTableSuffix();
        }
        return tableSuffix;
    }
}
