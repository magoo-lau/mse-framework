package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import vip.mseapp.api.system.RemoteUserService;
import vip.mseapp.mq.TenantEvent;
import vip.mseapp.mq.TenantEventSender;
import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.mapper.SysTenantMapper;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 租户异步开通器：管理员账号创建会首次路由到租户库，独立库场景触发全量表结构克隆
 * （{@link vip.mseapp.mybatis.datasource.TenantDsLazyInitializer}，耗时随业务积木增长且不可控），
 * 同步执行必然撞前端/RPC 超时——开通请求只登记任务即返回，耗时动作在本后台单线程执行；
 * 进度与结果落 sys_tenant.provision_status（0 初始化中 / 1 已开通 / 2 失败），
 * 管理端轮询租户列表即可看到状态流转，失败原因落 provision_error。
 *
 * <p>失败可重试（retryProvision → {@link #submit}）：任务幂等——管理员已在上次失败前创建成功则跳过创建，
 * 只补齐剩余步骤；密码明文不落库，重试时由运营重新录入。进程内 per-tenant 互斥防重复提交；
 * 多实例同时重试由 createTenantAdmin 的用户名唯一校验收敛（后到者落失败状态，可再重试）。
 */
public class TenantProvisioner {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioner.class);

    private static final int ERROR_MAX_LENGTH = 500;

    private final SysTenantMapper tenantMapper;

    /** 可空：租户事件发送器（进程内/MQ 双轨；缺失时回落进程内 Spring 事件） */
    private final ObjectProvider<TenantEventSender> eventSenderProvider;

    private final ApplicationEventPublisher eventPublisher;

    private final TenantRouteResolver routeResolver;

    /** 跨模块服务引用（单体本地直调 / 微服务 Dubbo 远程）；链路含独立库首次克隆，放宽默认 5s 超时 */
    @RpcReference(dubbo = @RpcReference.Dubbo(timeout = 120_000))
    private RemoteUserService remoteUserService;

    /** 进程内 per-tenant 互斥：同租户任务进行中拒绝重复提交 */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    /** 串行执行即可：开通是低频管理动作；独立库克隆自身另有 Redis 锁跨实例互斥 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tenant-provision");
        thread.setDaemon(true);
        return thread;
    });

    public TenantProvisioner(SysTenantMapper tenantMapper, TenantRouteResolver routeResolver,
                             ObjectProvider<TenantEventSender> eventSenderProvider,
                             ApplicationEventPublisher eventPublisher) {
        this.tenantMapper = tenantMapper;
        this.routeResolver = routeResolver;
        this.eventSenderProvider = eventSenderProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 登记开通任务并立即返回：先把租户标记为「初始化中」并记录管理员账号（重试回显），再投递后台执行。
     *
     * @throws ServiceException 同租户任务进行中
     */
    public void submit(Long tenantId, String adminUsername, String adminPassword) {
        if (!inFlight.add(tenantId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "该租户已有开通任务在执行，请稍后刷新查看状态");
        }
        try {
            mark(tenantId, adminUsername, 0, null);
        } catch (RuntimeException e) {
            inFlight.remove(tenantId);
            throw e;
        }
        executor.execute(() -> {
            try {
                provision(tenantId, adminUsername, adminPassword);
                mark(tenantId, null, 1, null);
                log.info("[mse-tenant] 租户后台开通完成: tenantId={}", tenantId);
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                mark(tenantId, null, 2, error.length() > ERROR_MAX_LENGTH ? error.substring(0, ERROR_MAX_LENGTH) : error);
                log.warn("[mse-tenant] 租户后台开通失败（可在「租户管理」重试开通）: tenantId={}", tenantId, e);
            } finally {
                inFlight.remove(tenantId);
            }
        });
    }

    /** 开通动作本体（幂等，重试即重跑）：管理员已存在则跳过创建，随后失效路由缓存 */
    private void provision(Long tenantId, String adminUsername, String adminPassword) {
        List<UserDTO> admins = remoteUserService.listTenantAdmins(tenantId);
        if (admins == null || admins.isEmpty()) {
            remoteUserService.createTenantAdmin(tenantId, adminUsername, MsePasswords.encode(adminPassword), adminUsername);
        }
        routeResolver.evict(tenantId);
        // 控制面 → 数据面：开通完成事件（数据面各服务据此初始化每租户种子数据/拉取快照）
        TenantEvent event = TenantEvent.of(tenantId, TenantEvent.Type.CREATED, tenantId);
        TenantEventSender sender = eventSenderProvider.getIfAvailable();
        if (sender != null) {
            sender.sendTenant(TenantEvent.TOPIC, tenantId, TenantEvent.Type.CREATED, tenantId);
        } else if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }

    /** 状态落库：非空字段走 entity（触发元对象填充），error 经 wrapper 显式置 null（updateById 忽略 null 字段） */
    private void mark(Long tenantId, String adminUsername, Integer provisionStatus, String provisionError) {
        SysTenant entity = new SysTenant();
        entity.setId(tenantId);
        entity.setAdminUsername(adminUsername);
        entity.setProvisionStatus(provisionStatus);
        tenantMapper.update(entity, new LambdaUpdateWrapper<SysTenant>()
                .eq(SysTenant::getId, tenantId)
                .set(SysTenant::getProvisionError, provisionError));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
