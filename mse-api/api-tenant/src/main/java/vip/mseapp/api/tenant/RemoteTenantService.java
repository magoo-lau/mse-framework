package vip.mseapp.api.tenant;

import vip.mseapp.api.tenant.dto.DomainConfigDTO;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.api.tenant.dto.TenantDatasourceDTO;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.api.tenant.dto.TenantSnapshotDTO;

import java.util.List;

/**
 * 租户服务契约（service-tenant 实现）。
 */
public interface RemoteTenantService {

    /** 按编号查询 */
    TenantDTO getTenantById(Long tenantId);

    /** 按编码查询（登录页支持租户编码定位） */
    TenantDTO getTenantByCode(String tenantCode);

    /** 按绑定域名查询（登录页域名解析；null 表示该域名未绑定任何租户） */
    TenantDTO getTenantByDomain(String domain);

    /**
     * 按域名查询登录页个性化配置（标题/Logo/背景/版权）。
     *
     * @param domain 完整主机名（小写；登录页公开接口入参）
     * @return 启用中的配置；未配置或已停用返回 null（登录页回退默认样式）
     */
    DomainConfigDTO getDomainConfig(String domain);

    /**
     * 登录链路租户校验：存在性、状态、有效期。
     *
     * @throws vip.mseapp.core.exception.ServiceException 校验失败（TENANT_NOT_EXISTS / TENENT_DISABLED / TENANT_EXPIRED）
     */
    TenantDTO validateTenant(Long tenantId);

    /**
     * 租户套餐圈定的菜单编号集合（权限域裁剪依据；子应用可用性亦由该集合推导）。
     *
     * @return null 表示未配套餐（宽松默认，不裁剪）；空列表表示套餐未勾选任何菜单
     */
    List<Long> getPackageMenuIds(Long tenantId);

    /**
     * 全量租户列表（管理端下拉选择等场景；含停用租户，按 id 升序）。
     */
    List<TenantDTO> listTenants();

    /**
     * 租户×服务路由解析（多模式：独立库 / 共享库分表 / 共享库字段隔离 + 表级策略）。
     *
     * @param tenantId    租户编号
     * @param serviceName 服务名（调用方 spring.application.name）
     * @return 路由快照，永不为 null（未配置返回 SHARED_FIELD 缺省快照）
     */
    TenantRouteDTO getTenantRoute(Long tenantId, String serviceName);

    /**
     * 租户快照（控制面 → 数据面下发副本的拉取通道）：事件丢失/首次自举时数据面低频拉取，
     * 平台侧 AES-GCM 加密 + HMAC 签名后由数据面验签落库。运行期数据面本地自治，不依赖本接口。
     */
    TenantSnapshotDTO getTenantSnapshot(Long tenantId);

    /**
     * 取租户权益快照（0.1.0 权益模型：菜单域 + 配额/开关链，永不为 null；
     * 消费方应经 EntitlementCache 版本缓存，运行期不直调）。
     */
    vip.mseapp.api.tenant.dto.EntitlementDTO getTenantEntitlement(Long tenantId);

    /**
     * 本服务相关的租户数据源定义（service_name ∈ (serviceName, '*')，启用 + 停用行均返回）。
     * password 为密文（各服务以共享 encrypt-key 解密）。
     */
    List<TenantDatasourceDTO> listTenantDatasources(String serviceName);

    /** 按 dsKey 精确取数据源定义（变更广播回查）；不存在返回 null */
    TenantDatasourceDTO getTenantDatasource(String dsKey);
}
