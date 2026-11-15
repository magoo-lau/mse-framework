package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.api.system.RemoteAuthService;
import vip.mseapp.api.system.RemoteUserService;
import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.redis.MsePermCacheKeys;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.service.TenantProvisioner;
import vip.mseapp.service.platform.service.SysTenantRouteService;
import vip.mseapp.service.platform.service.SysTenantService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

// 控制面服务（sys_tenant 仅平台库存在）：类级 NOT_SUPPORTED 挂起调用方可能持有的租户事务——
// 事务 doBegin 已把物理连接钉死在租户库，控制面查询跟随绑定连接即报
// 「Table '租户库.sys_tenant' doesn't exist」（新增角色 createRole → getPackageMenuIds 即中招），
// 挂起后按平台实体钉库执行。防错库堤坝，勿删（范式详见 SysTenantRouteServiceImpl 类注释）。
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SysTenantServiceImpl extends CrudRepository<SysTenantMapper, SysTenant> implements SysTenantService {

    /** 绑定域名格式：完整主机名（至少含一个点），与 Controller 侧校验保持一致 */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$");

    /**
     * 跨模块服务引用：单体下本地直调，微服务下 Dubbo 远程。
     * timeout 放宽到 120s：listTenantAdmins/resetTenantAdminPassword 按路由查租户库，
     * 独立库初始化进行中时会同步等待首次克隆（TenantDsLazyInitializer，Redis 锁 TTL 120s），
     * 默认 5s 撑不住；开通动作本体已移交 TenantProvisioner（自带同款放宽引用）
     */
    @RpcReference(dubbo = @RpcReference.Dubbo(timeout = 120_000))
    private RemoteUserService remoteUserService;

    /** 双因子重置链路（租户管理员丢失验证器时由平台运营重置；轻量操作无需放宽 timeout） */
    @RpcReference
    private RemoteAuthService remoteAuthService;

    private final TenantRouteResolver routeResolver;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysTenantRouteService routeService;
    private final TenantProvisioner provisioner;

    /** 0.1.0 权益模型：换绑套餐失效钩子（可空——测试装配可省） */
    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider;

    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider;

    public SysTenantServiceImpl(TenantRouteResolver routeResolver,
                                RedisTemplate<String, Object> redisTemplate,
                                SysTenantRouteService routeService,
                                TenantProvisioner provisioner,
                               org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider,
                               org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider) {
        this.routeResolver = routeResolver;
        this.redisTemplate = redisTemplate;
        this.routeService = routeService;
        this.provisioner = provisioner;
        this.entitlementProvider = entitlementProvider;
        this.eventSenderProvider = eventSenderProvider;
    }

    @Override
    // sys_tenant 落主库与后台开通任务分属不同事务边界，本地事务只保证本行写入；
    // 管理员创建/独立库克隆按路由落在共享池或租户库，由 TenantProvisioner 后台完成并回写开通状态
    public Long createTenant(SysTenant tenant, String adminUsername, String adminPassword) {
        Long count = lambdaQuery().eq(SysTenant::getCode, tenant.getCode()).count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_TENANT_CODE_DUPLICATE);
        }
        // 全局默认绑定（sys_tenant.ds_key）：空白=未绑定（各服务本地内置共享库，mse.tenant.shared-main.*）；
        // 提供时做存在性/启用/独立库独占校验
        if (StringUtils.hasText(tenant.getDsKey())) {
            routeService.validateDefaultBinding(tenant.getDsKey().trim(), null);
            tenant.setDsKey(tenant.getDsKey().trim());
        } else {
            tenant.setDsKey(null);
        }
        tenant.setDomain(normalizeDomain(tenant.getDomain()));
        checkDomainUnique(tenant.getDomain(), null);
        tenant.setId(null);
        tenant.setStatus(0);
        // 先落「初始化中」即返回：管理员创建会首次路由到租户库（独立库触发全量表克隆），
        // 耗时随业务积木增长不可控，异步执行避免前端/RPC 超时
        tenant.setProvisionStatus(0);
        save(tenant);
        provisioner.submit(tenant.getId(), adminUsername, adminPassword);
        routeResolver.evict(tenant.getId());
        return tenant.getId();
    }

    @Override
    public void retryProvision(Long tenantId, String adminUsername, String adminPassword) {
        SysTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        // 已开通（含存量 NULL 视同完成）不允许重跑；「初始化中」放行以自愈进程重启导致的任务丢失
        // （真在执行中会被 TenantProvisioner 的 per-tenant 互斥拒绝）
        if (tenant.getProvisionStatus() == null || Integer.valueOf(1).equals(tenant.getProvisionStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该租户已开通完成，无需重试");
        }
        provisioner.submit(tenantId, adminUsername.trim(), adminPassword);
    }

    @Override
    public PageResult<SysTenant> pageTenants(long pageNum, long pageSize, String name, Integer status) {
        Page<SysTenant> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysTenant>()
                        .like(StringUtils.hasText(name), SysTenant::getName, name)
                        .eq(status != null, SysTenant::getStatus, status)
                        .orderByDesc(SysTenant::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public void updateTenant(SysTenant tenant) {
        tenant.setCode(null);
        // 开通元数据由开通链路维护，不允许经编辑入口变更（updateById 忽略 null 字段）
        tenant.setAdminUsername(null);
        tenant.setProvisionStatus(null);
        tenant.setProvisionError(null);
        String domain = normalizeDomain(tenant.getDomain());
        checkDomainUnique(domain, tenant.getId());
        // 全局默认绑定：非空校验（存在、启用、独立库独占）；空白归 null（存量兼容，界面警示）
        String dsKey = StringUtils.hasText(tenant.getDsKey()) ? tenant.getDsKey().trim() : null;
        routeService.validateDefaultBinding(dsKey, tenant.getId());
        // updateById 忽略 null 字段，无法表达"清空绑定"，域名/数据源/套餐改由下方显式 set 同步
        tenant.setDomain(null);
        tenant.setDsKey(null);
        Long packageId = tenant.getPackageId();
        tenant.setPackageId(null);
        updateById(tenant);
        lambdaUpdate()
                .eq(SysTenant::getId, tenant.getId())
                .set(SysTenant::getDomain, domain)
                .set(SysTenant::getDsKey, dsKey)
                .set(SysTenant::getPackageId, packageId)
                .update();
        routeResolver.evict(tenant.getId());
        // 换绑数据源 = 路由变更：广播数据面失效快照（微服务形态各业务实例秒级感知，不等 TTL）
        publishRouteChanged(tenant.getId());
        // 套餐归属可能变更：踢该租户的权限缓存版本，使域裁剪即刻生效
        redisTemplate.opsForValue().increment(MsePermCacheKeys.tenantVer(tenant.getId()));
        // 0.1.0 权益模型：换绑套餐即权益链变更——失效租户权益缓存 + FEATURE_CHANGED 事件
        if (entitlementProvider != null) {
            vip.mseapp.service.platform.service.EntitlementService entitlement = entitlementProvider.getIfAvailable();
            if (entitlement != null) {
                entitlement.evictTenant(tenant.getId());
            }
        }
    }

    /** 路由变更广播（数据面快照秒级失效；发送失败仅告警，数据面有版本校验/TTL 兜底） */
    private void publishRouteChanged(Long tenantId) {
        vip.mseapp.mq.TenantEventSender sender = eventSenderProvider == null ? null : eventSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            sender.sendTenant(vip.mseapp.mq.TenantEvent.TOPIC, tenantId,
                    vip.mseapp.mq.TenantEvent.Type.ROUTE_CHANGED, "tenant-ds-rebind");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(SysTenantServiceImpl.class)
                    .warn("[mse-tenant] 换绑路由变更事件广播失败（数据面经版本校验/TTL 兜底）: tenant={} {}", tenantId, e.getMessage());
        }
    }

    @Override
    public void changeStatus(Long tenantId, Integer status) {
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setStatus(status);
        updateById(update);
        // 停启用影响登录校验/租户行缓存与路由解析：本进程即时失效 + INCR 租户版本（跨实例 ≤30s 收敛）
        routeResolver.evict(tenantId);
    }

    @Override
    public SysTenant validateTenant(Long tenantId) {
        SysTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new ServiceException(GlobalErrorCode.TENANT_NOT_EXISTS);
        }
        if (Integer.valueOf(1).equals(tenant.getStatus())) {
            throw new ServiceException(GlobalErrorCode.TENENT_DISABLED);
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(GlobalErrorCode.TENANT_EXPIRED);
        }
        // 0.1.0 内置缺省共享库：ds_key 空 = 各服务本地内置共享库（mse.tenant.shared-main.* 每服务
        // 自declare，键默认=本服务应用名）——合法绑定态，路由兜底 dsKey=null 由各服务本地解析；
        // 服务未配置坐标时语句快速失败（报错可见）。独立库租户仍要求显式绑定。
        return tenant;
    }

    @Override
    public void resetAdminPassword(Long tenantId, String username, String newPassword) {
        if (tenantId == null || !StringUtils.hasText(username)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "tenantId 与管理员账号不能为空");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.trim().length() < 6 || newPassword.length() > 32) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "新密码长度须在 6~32 位之间");
        }
        // 一次性开通写入：按开通登记的账号经契约下发（平台不回读租户库），BCrypt 散列在此完成
        remoteUserService.resetTenantAdminPassword(tenantId, username.trim(), MsePasswords.encode(newPassword));
    }

    @Override
    public void resetAdminTotp(Long tenantId, String username) {
        if (tenantId == null || !StringUtils.hasText(username)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "tenantId 与管理员账号不能为空");
        }
        remoteAuthService.resetTenantAdminTotp(tenantId, username.trim());
    }

    /** 规范化绑定域名：trim + 小写，空白归一为 null（= 不绑定/解绑）；格式非法抛 400 */
    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        String normalized = domain.trim().toLowerCase();
        if (!DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "绑定域名格式不正确（完整主机名，如 demo.example.com）");
        }
        return normalized;
    }

    /** 域名唯一性校验；excludeId 用于更新时排除自身 */
    private void checkDomainUnique(String domain, Long excludeId) {
        if (domain == null) {
            return;
        }
        Long count = lambdaQuery()
                .eq(SysTenant::getDomain, domain)
                .ne(excludeId != null, SysTenant::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_TENANT_DOMAIN_DUPLICATE);
        }
    }
}
