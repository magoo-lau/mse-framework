package vip.mseapp.service.system.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import vip.mseapp.redis.config.MseRedisAutoConfiguration;
import vip.mseapp.service.system.service.TenantBrandingCachePublisher;

/**
 * 租户域品牌缓存启动重建 Runner：应用就绪即把全部租户的域名绑定与自助品牌
 * （含控制面遗留单域名）注册进 Redis 租户域 hash，登录页解析只读缓存。
 *
 * <p>多实例经分布式锁只重建一份；单租户失败不阻断启动（读取侧 DB 兜底 +
 * 按 key 惰性回填，详见 {@link TenantBrandingCachePublisher}）。
 */
public class TenantBrandingCacheRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantBrandingCacheRunner.class);

    /** 重建锁：抢不到说明其他实例正在重建，共享 hash 无需重做 */
    public static final String REBUILD_LOCK_KEY = "mse:branding:rebuild:tenant";

    private final TenantBrandingCachePublisher publisher;
    private final ObjectProvider<MseRedisAutoConfiguration.LockOps> lockOpsProvider;

    public TenantBrandingCacheRunner(TenantBrandingCachePublisher publisher,
                                     ObjectProvider<MseRedisAutoConfiguration.LockOps> lockOpsProvider) {
        this.publisher = publisher;
        this.lockOpsProvider = lockOpsProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        MseRedisAutoConfiguration.LockOps lockOps = lockOpsProvider.getIfAvailable();
        if (lockOps == null) {
            log.debug("[mse-branding-cache] Redis 不可用，租户域品牌缓存重建跳过");
            return;
        }
        try {
            boolean executed = lockOps.tryExecute(REBUILD_LOCK_KEY, publisher::rebuildAll);
            if (!executed) {
                log.info("[mse-branding-cache] 其他实例正在重建租户域品牌缓存，本实例跳过");
            }
        } catch (Exception ex) {
            // Redis 异常降级：登录页解析走 DB 兜底，不阻断启动
            log.warn("[mse-branding-cache] 租户域品牌缓存启动重建失败（读取侧 DB 兜底）: {}", ex.getMessage());
        }
    }
}
