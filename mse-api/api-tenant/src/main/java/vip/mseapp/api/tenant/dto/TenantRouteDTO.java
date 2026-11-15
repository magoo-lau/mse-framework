package vip.mseapp.api.tenant.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 租户×服务路由快照（内部 RPC 契约，getTenantRoute 返回）。
 *
 * <p>解析顺序：sys_tenant_service_config(tenant, service) → sys_tenant.ds_key 全局默认
 * （独立库语义）→ 无配置 = SHARED_FIELD 缺省快照。
 */
public class TenantRouteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private String serviceName;

    /** 隔离模式：INDEPENDENT_DB / SHARED_TABLE_SPLIT / SHARED_FIELD（永不为空，缺省 SHARED_FIELD） */
    private String mode;

    /** 路由数据源 key；null = 不路由（落本服务业务主库） */
    private String dsKey;

    /** 服务级分表后缀（SHARED_TABLE_SPLIT 缺省） */
    private String tableSuffix;

    /** 字段隔离列名（服务级，缺省 tenant_id） */
    private String tenantColumn;

    /** 表级策略（覆盖服务级默认） */
    private List<TablePolicyDTO> tablePolicies;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }

    public void setTableSuffix(String tableSuffix) {
        this.tableSuffix = tableSuffix;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public List<TablePolicyDTO> getTablePolicies() {
        return tablePolicies;
    }

    public void setTablePolicies(List<TablePolicyDTO> tablePolicies) {
        this.tablePolicies = tablePolicies;
    }
}
