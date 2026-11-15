package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import vip.mseapp.api.system.RemoteBaseDataRegistry;
import vip.mseapp.api.system.dto.BaseScriptDescriptor;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.core.schedule.TaskDefinition;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基座库脚本推送器（base 通道）：应用就绪后把本应用 classpath 上各积木自带的
 * {@code db/<方言>/} 中 target=base 的脚本（子应用注册/菜单/权限点等基座库幂等脚本）推送至
 * mse-system（STARTUP 一次性任务 + 成功前退避重试）。
 *
 * <p>jar 内脚本在构建期冻结、运行期不可变，因此<b>不做周期重推</b>——启动推送一次，失败（对端
 * 未就绪/网络/接收端长持锁）按退避间隔重试至首次成功即停止；极端情况超过重试窗口时，可经平台端
 * 「任务调度」页对 {@code bridge-base-data-push} 手动 trigger 补推。送达语义 = at-least-once
 * 推送 + 接收端账本幂等收敛到 exactly-once 应用。
 *
 * <p>注册通道按 {@code mse.rpc.mode} 解析，与基座 RPC 双形态一致：
 * <ul>
 *   <li>local（单体）：容器内直调 {@link RemoteBaseDataRegistry} 唯一实现（聚合了
 *       service-system 即存在）；不存在说明宿主未组装基座系统服务，告警一次后停止重试；</li>
 *   <li>dubbo（微服务）：经 {@link BaseDataDubboRegistryReference} 远调 mse-system
 *       （本类保持零 Dubbo import，与开放平台同款探测门控），check=false 启动不依赖对方就绪。</li>
 * </ul>
 * 调度经统一调度中心（taskId=bridge-base-data-push，STARTUP 型；多实例防重——接收端幂等，防重
 * 只为省流量），classpath 无 target=base 脚本则不注册（零副作用）。推送内容全方言一并发出，
 * 接收端按自身数据库方言筛取。
 */
public class BaseDataScriptPusher implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    /** 任务编号（平台端「任务调度」页可见，可手动 trigger 补推） */
    static final String TASK_ID = "bridge-base-data-push";

    private static final Logger log = LoggerFactory.getLogger(BaseDataScriptPusher.class);

    /** 已知方言（与 TenantMigrationScanner/TenantSchemaMigrator 一致） */
    private static final List<String> DIALECTS = List.of("mysql", "dm", "kingbase");

    private static final String LOCATION_PATTERN = "classpath*:db/%s/V*.sql";

    /** 重试退避：30s 起步 ×1.5 递增、上限 120s、至多 20 次（合计约 28 分钟窗口） */
    private static final long RETRY_INITIAL_MS = 30_000L;

    private static final long RETRY_MAX_MS = 120_000L;

    private static final double RETRY_BACKOFF_FACTOR = 1.5;

    private static final int RETRY_MAX_ATTEMPTS = 20;

    private final Environment environment;

    private final org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory;

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    /** 扫描器自带按方言缓存（classpath 资源运行期不变），构造时创建、全程复用 */
    private final TenantMigrationScanner scanner = new TenantMigrationScanner(LOCATION_PATTERN);

    /** local 模式解析结果：宿主未组装基座系统服务时置 localAbsent 哨兵，停止重试 */
    private volatile RemoteBaseDataRegistry localRegistry;

    private volatile boolean localAbsent;

    private final AtomicBoolean localAbsentWarned = new AtomicBoolean(false);

    public BaseDataScriptPusher(Environment environment,
                                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory,
                                ObjectProvider<MseTaskScheduler> schedulerProvider) {
        this.environment = environment;
        this.beanFactory = beanFactory;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (scan().isEmpty()) {
            log.debug("[mse-bridge] classpath 无 db/base 脚本，基座库脚本推送器闲置");
            return;
        }
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.info("[mse-bridge] 调度器未装配（mse.scheduling.mode=none 等），基座库脚本推送不启动");
            return;
        }
        scheduler.register(TaskDefinition.startup(TASK_ID, "基座库脚本推送",
                        "启动推送 db/base 脚本至 mse-system（成功前退避重试；菜单/权限/子应用注册），"
                                + "失败超过重试窗口可手动触发补推")
                        .category(TaskDefinition.CATEGORY_PLATFORM),
                this::pushWithRetry);
    }

    /** STARTUP 任务体：退避重试至首次成功（或确认无事可做/对端永久缺席）即收敛 */
    private void pushWithRetry() throws InterruptedException {
        long delay = RETRY_INITIAL_MS;
        for (int attempt = 1; attempt <= RETRY_MAX_ATTEMPTS; attempt++) {
            Outcome outcome;
            try {
                outcome = push();
            } catch (Throwable e) {
                log.warn("[mse-bridge] 基座库脚本推送异常（第 {}/{} 次）: {}", attempt, RETRY_MAX_ATTEMPTS, e.getMessage());
                outcome = Outcome.RETRY;
            }
            if (outcome != Outcome.RETRY) {
                return;
            }
            if (attempt < RETRY_MAX_ATTEMPTS) {
                log.info("[mse-bridge] 基座库脚本推送未完成，{}s 后重试（第 {}/{} 次）",
                        delay / 1000, attempt + 1, RETRY_MAX_ATTEMPTS);
                Thread.sleep(delay);
                delay = Math.min((long) (delay * RETRY_BACKOFF_FACTOR), RETRY_MAX_MS);
            }
        }
        log.error("[mse-bridge] 基座库脚本推送 {} 次均未成功，已停止重试——请排查 mse-system 可用性后"
                + "经平台端「任务调度」页手动触发 {} 补推", RETRY_MAX_ATTEMPTS, TASK_ID);
    }

    /** 单次推送结果：SUCCESS=已送达或无事可做；STOP=对端永久缺席（local 未组装）；RETRY=可重试失败 */
    private enum Outcome {
        SUCCESS, STOP, RETRY
    }

    private Outcome push() {
        List<BaseScriptDescriptor> scripts = scan();
        if (scripts.isEmpty()) {
            return Outcome.SUCCESS;
        }
        RemoteBaseDataRegistry registry = resolveRegistry();
        if (registry == null) {
            return localAbsent ? Outcome.STOP : Outcome.RETRY; // dubbo 对端未就绪（重试）/ local 已告警停推
        }
        String summary = registry.register(scripts);
        if (summary != null && summary.contains("跳过")) {
            // 接收端长持锁等罕见冲突：按失败处理走退避重试
            log.warn("[mse-bridge] 基座库脚本推送被接收端跳过，稍后重试：{}", summary.replace("\n", "；"));
            return Outcome.RETRY;
        }
        log.info("[mse-bridge] 已推送 {} 条基座库脚本至 mse-system：{}", scripts.size(), summary);
        return Outcome.SUCCESS;
    }

    private List<BaseScriptDescriptor> scan() {
        List<BaseScriptDescriptor> scripts = new ArrayList<>();
        for (String dialect : DIALECTS) {
            for (SchemaMigration migration : scanner.scan(dialect)) {
                if (migration.target() != MigrationTarget.BASE || migration.perTenant()) {
                    // 只推 base 通道；master/platform 通道由启动迁移与平台侧负责
                    continue;
                }
                BaseScriptDescriptor descriptor = new BaseScriptDescriptor();
                descriptor.setBiz(migration.biz());
                descriptor.setVersion(migration.version());
                descriptor.setDialect(dialect);
                descriptor.setScript(migration.script());
                descriptor.setSql(migration.sql());
                descriptor.setChecksum(migration.checksum());
                descriptor.setBaseScope(migration.baseScope().name());
                scripts.add(descriptor);
            }
        }
        return scripts;
    }

    private RemoteBaseDataRegistry resolveRegistry() {
        if ("dubbo".equalsIgnoreCase(environment.getProperty("mse.rpc.mode", "local"))
                && dubboPresent()) {
            // 仅在 dubbo 分支触达 BaseDataDubboRegistryReference（宿主无 Dubbo 时该类不被加载）
            return BaseDataDubboRegistryReference.proxy(environment);
        }
        if (localRegistry == null && !localAbsent) {
            synchronized (this) {
                if (localRegistry == null && !localAbsent) {
                    var candidates = beanFactory.getBeansOfType(RemoteBaseDataRegistry.class);
                    if (candidates.isEmpty()) {
                        localAbsent = true;
                        if (localAbsentWarned.compareAndSet(false, true)) {
                            log.warn("[mse-bridge] 容器内无 RemoteBaseDataRegistry 实现（宿主未聚合 service-system），"
                                    + "跳过基座库脚本推送（菜单/权限/子应用注册请回退手工 SQL 或后台配置）");
                        }
                    } else if (candidates.size() > 1) {
                        localAbsent = true;
                        log.warn("[mse-bridge] 容器内存在多个 RemoteBaseDataRegistry 实现 {}，跳过基座库脚本推送",
                                candidates.keySet());
                    } else {
                        localRegistry = candidates.values().iterator().next();
                    }
                }
            }
        }
        return localRegistry;
    }

    private boolean dubboPresent() {
        try {
            Class.forName("org.apache.dubbo.config.ReferenceConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        if ("dubbo".equalsIgnoreCase(environment.getProperty("mse.rpc.mode", "local"))
                && dubboPresent()) {
            BaseDataDubboRegistryReference.destroy();
        }
    }
}
