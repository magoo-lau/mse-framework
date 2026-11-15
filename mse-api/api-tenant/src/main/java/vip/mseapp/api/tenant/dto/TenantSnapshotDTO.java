package vip.mseapp.api.tenant.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 租户快照（控制面 → 数据面下发副本的载荷）：租户元数据 + 逐服务路由 + 套餐菜单域 + 配额。
 *
 * <p>平台侧构建并 AES-GCM 加密 + HMAC 签名后经事件/拉取通道下发；数据面验签解密后
 * 落本地库（sys_tenant_snapshot）+ 内存缓存，运行期鉴权/路由/裁剪全部本地自治，
 * 平台故障不影响已开通租户。
 */
public class TenantSnapshotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private String code;

    private String name;

    /** 绑定域名（null=未绑定） */
    private String domain;

    /** 状态：0 启用 / 1 停用 */
    private Integer status;

    /** 独立库数据源 key 全局默认（null=共享库） */
    private String dsKey;

    /** 套餐菜单域（租户可见菜单 id；null=未配套餐，宽松默认） */
    private List<Long> packageMenuIds;

    /** 逐服务路由快照（含 "*" 全局缺省项） */
    private List<TenantRouteDTO> routes;

    /** 配额（预留：maxUsers 等键值对，平台定义、数据面执行） */
    private Map<String, String> quotas;

    /** 快照版本（平台发布时间戳，单调递增；数据面据此判断新旧） */
    private Long snapshotVersion;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }

    public List<Long> getPackageMenuIds() {
        return packageMenuIds;
    }

    public void setPackageMenuIds(List<Long> packageMenuIds) {
        this.packageMenuIds = packageMenuIds;
    }

    public List<TenantRouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<TenantRouteDTO> routes) {
        this.routes = routes;
    }

    public Map<String, String> getQuotas() {
        return quotas;
    }

    public void setQuotas(Map<String, String> quotas) {
        this.quotas = quotas;
    }

    /** 功能开关（feature.* → 值） */
    private Map<String, String> features;

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }

    public Long getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(Long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }
}
