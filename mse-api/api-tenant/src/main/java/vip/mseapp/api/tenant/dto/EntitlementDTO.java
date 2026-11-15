package vip.mseapp.api.tenant.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户权益快照（0.1.0 权益模型）：权益 = 套餐 menuIds（菜单域）+ 权益项链
 * （default → 套餐 → 附加项×qty → 租户覆盖，后者优先）。
 *
 * <p>menuIds 语义与 {@link RemoteTenantService#getPackageMenuIds} 一致：null=未配套餐（宽松全量）；
 * quotas/features 为「编码→值」映射（quota.* / feature.*）。控制面计算（EntitlementService）、
 * 快照携带（getTenantSnapshot）、数据面经 EntitlementCache / TenantSnapshotStore 本地消费。
 */
public class EntitlementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    /** 套餐菜单域（逗号串拆解；null=未配套餐宽松全量） */
    private List<Long> menuIds;

    /** 配额项（quota.* → 值；附加项配额已按 qty 累加） */
    private Map<String, String> quotas = new LinkedHashMap<>();

    /** 功能开关（feature.* → 值） */
    private Map<String, String> features = new LinkedHashMap<>();

    /** 计算版本（毫秒时间戳，单调递增） */
    private long version;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    public Map<String, String> getQuotas() {
        return quotas;
    }

    public void setQuotas(Map<String, String> quotas) {
        this.quotas = quotas;
    }

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
