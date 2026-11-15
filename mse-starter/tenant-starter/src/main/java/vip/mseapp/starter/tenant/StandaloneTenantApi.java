package vip.mseapp.starter.tenant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.DomainConfigDTO;
import vip.mseapp.api.tenant.dto.EntitlementDTO;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.api.tenant.dto.TenantDatasourceDTO;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.api.tenant.dto.TenantSnapshotDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.license.LicensePayload;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.system.security.LicenseHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RemoteTenantService 的 standalone 本地桩：脱离平台后契约消费全部本地闭环——
 * <ul>
 *   <li>租户查询/校验：本地 sys_tenant 权威行（{@link StandaloneBootstrap} 启动写入），
 *       经 {@link TenantCache} 走 Redis 缓存（standalone 默认仅此一条自身租户缓存，
 *       启动/离线交付导入时失效，运行期由 TTL + 版本号兜底），miss/Redis 不可用回源 JDBC；</li>
 *   <li>套餐权限域：license 凭证 menuIds（空载荷=空域，绝不宽松放行——防伪造关键）；</li>
 *   <li>权益快照：license 凭证 features 离线分桶（quota.*→quotas、其余→features，签发时从平台权益链灌入）；</li>
 *   <li>路由：固定 SHARED_FIELD 缺省快照（单库形态不路由，全部落 master=租户库）；</li>
 *   <li>数据源/快照/域名：standalone 无控制面，返回空。</li>
 * </ul>
 *
 * <p>注册为同契约 Bean 后，starter-bridge 的 RPC 版实现（{@code @ConditionalOnMissingBean}）自动让位；
 * mono-starter（含 service-platform）不引本模块，两形态互斥。
 */
@RpcService(RemoteTenantService.class)
public class StandaloneTenantApi implements RemoteTenantService {

    private final JdbcTemplate jdbcTemplate;
    private final LicenseHolder licenseHolder;
    private final ObjectProvider<TenantCache> tenantCacheProvider;
    private final long tenantId;

    public StandaloneTenantApi(JdbcTemplate jdbcTemplate, LicenseHolder licenseHolder,
                               ObjectProvider<TenantCache> tenantCacheProvider,
                               @org.springframework.beans.factory.annotation.Value("${mse.standalone.tenant-id:1}") long tenantId) {
        this.jdbcTemplate = jdbcTemplate;
        this.licenseHolder = licenseHolder;
        this.tenantCacheProvider = tenantCacheProvider;
        this.tenantId = tenantId;
    }

    @Override
    public TenantDTO getTenantById(Long tenantId) {
        return loadDto(tenantId);
    }

    @Override
    public TenantDTO getTenantByCode(String tenantCode) {
        TenantDTO tenant = loadDto(tenantId);
        if (!tenant.getCode().equals(tenantCode)) {
            throw new ServiceException(GlobalErrorCode.TENANT_NOT_EXISTS);
        }
        return tenant;
    }

    @Override
    public TenantDTO getTenantByDomain(String domain) {
        // standalone 无域名绑定（登录页域名个性化返回默认样式）
        return null;
    }

    @Override
    public DomainConfigDTO getDomainConfig(String domain) {
        return null;
    }

    @Override
    public TenantDTO validateTenant(Long tenantId) {
        TenantDTO tenant = loadDto(tenantId);
        if (Integer.valueOf(1).equals(tenant.getStatus())) {
            throw new ServiceException(GlobalErrorCode.TENENT_DISABLED);
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(GlobalErrorCode.TENANT_EXPIRED);
        }
        // 授权凭证锁定：缺失/验签失败/超宽限 = 登录锁定（防伪造菜单权限访问套餐外功能的最终闸门）
        LicenseHolder.State state = licenseHolder.state();
        if (state == LicenseHolder.State.MISSING || state == LicenseHolder.State.EXPIRED) {
            throw new ServiceException(423, "系统授权已失效，请联系平台运营续期（" + licenseHolder.graceMessage() + "）");
        }
        return tenant;
    }

    @Override
    public List<Long> getPackageMenuIds(Long tenantId) {
        // 域真源 = 平台签发凭证（绝不返回 null 宽松放行；空载荷=全部菜单不可用）
        LicenseHolder.State state = licenseHolder.state();
        if (state == LicenseHolder.State.MISSING || state == LicenseHolder.State.EXPIRED) {
            return List.of();
        }
        return licenseHolder.payload() == null ? List.of() : licenseHolder.payload().menuIds();
    }

    @Override
    public EntitlementDTO getTenantEntitlement(Long tenantId) {
        // standalone 离线权益快照：菜单域=凭证 menuIds（同 getPackageMenuIds 防伪口径）；
        // 配额/开关=凭证 features（签发时从平台权益链全量灌入，与 LicenseTenantScopeService 同源）
        EntitlementDTO entitlement = new EntitlementDTO();
        entitlement.setTenantId(tenantId);
        entitlement.setMenuIds(getPackageMenuIds(tenantId));
        LicensePayload payload = licenseHolder.payload();
        if (payload != null && payload.features() != null) {
            for (Map.Entry<String, String> entry : payload.features().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                if (entry.getKey() != null && entry.getKey().startsWith("quota.")) {
                    entitlement.getQuotas().put(entry.getKey(), entry.getValue());
                } else {
                    entitlement.getFeatures().put(entry.getKey(), entry.getValue());
                }
            }
        }
        entitlement.setVersion(System.currentTimeMillis());
        return entitlement;
    }

    @Override
    public List<TenantDTO> listTenants() {
        return List.of(loadDto(tenantId));
    }

    @Override
    public TenantRouteDTO getTenantRoute(Long tenantId, String serviceName) {
        // 单库形态固定字段隔离缺省快照：路由拦截器对 dsKey=null 不切库，全部落 master（租户库）
        TenantRouteDTO route = new TenantRouteDTO();
        route.setTenantId(tenantId);
        route.setServiceName(serviceName);
        route.setMode("SHARED_FIELD");
        return route;
    }

    @Override
    public TenantSnapshotDTO getTenantSnapshot(Long tenantId) {
        return null;
    }

    @Override
    public List<TenantDatasourceDTO> listTenantDatasources(String serviceName) {
        return List.of();
    }

    @Override
    public TenantDatasourceDTO getTenantDatasource(String dsKey) {
        return null;
    }

    /**
     * 本租户行（Redis 缓存优先，standalone 仅一条自身租户条目；miss 或 Redis 不可用回源本地 sys_tenant）。
     * 缓存载体为 TenantDTO——LocalTenant 是 record（final 类），默认类型序列化不写 @class，读回无法还原。
     */
    private TenantDTO loadDto(Long id) {
        if (id == null || id != tenantId) {
            // standalone 仅一个租户：其余编号一律不存在（防跨租户探测）
            throw new ServiceException(GlobalErrorCode.TENANT_NOT_EXISTS);
        }
        TenantCache cache = tenantCacheProvider.getIfAvailable();
        if (cache != null) {
            return cache.getTenantInfo(tenantId, TenantDTO.class,
                    () -> toDto(StandaloneBootstrap.loadLocalTenant(jdbcTemplate, tenantId)));
        }
        return toDto(StandaloneBootstrap.loadLocalTenant(jdbcTemplate, tenantId));
    }

    private TenantDTO toDto(StandaloneBootstrap.LocalTenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.id());
        dto.setCode(tenant.code());
        dto.setName(tenant.name());
        dto.setDsKey("standalone");
        dto.setStatus(tenant.status());
        dto.setExpireTime(tenant.expireTime());
        return dto;
    }
}
