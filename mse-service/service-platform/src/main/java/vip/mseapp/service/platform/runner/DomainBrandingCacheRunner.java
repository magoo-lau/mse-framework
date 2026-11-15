package vip.mseapp.service.platform.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.DomainBrandingEntry;
import vip.mseapp.redis.config.MseRedisAutoConfiguration;
import vip.mseapp.service.platform.entity.SysDomainConfig;
import vip.mseapp.service.platform.service.SysDomainConfigService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台域品牌缓存启动重建 Runner：应用就绪即把平台自有域名个性化
 * （sys_domain_config 启用行，如管理端 admin.mse.local）注册进 Redis 平台域 hash，
 * 登录页解析只读缓存。租户站点域名不在本 Runner 职责内（service-system 租户域通道）。
 *
 * <p>多实例经分布式锁只重建一份；Redis 异常降级不阻断启动（读取侧 DB 兜底）。
 */
public class DomainBrandingCacheRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainBrandingCacheRunner.class);

    /** 重建锁：抢不到说明其他实例正在重建，共享 hash 无需重做 */
    public static final String REBUILD_LOCK_KEY = "mse:branding:rebuild:platform";

    private final SysDomainConfigService domainConfigService;
    private final ObjectProvider<DomainBrandingCache> cacheProvider;
    private final ObjectProvider<MseRedisAutoConfiguration.LockOps> lockOpsProvider;

    public DomainBrandingCacheRunner(SysDomainConfigService domainConfigService,
                                     ObjectProvider<DomainBrandingCache> cacheProvider,
                                     ObjectProvider<MseRedisAutoConfiguration.LockOps> lockOpsProvider) {
        this.domainConfigService = domainConfigService;
        this.cacheProvider = cacheProvider;
        this.lockOpsProvider = lockOpsProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        DomainBrandingCache cache = cacheProvider.getIfAvailable();
        MseRedisAutoConfiguration.LockOps lockOps = lockOpsProvider.getIfAvailable();
        if (cache == null || lockOps == null) {
            log.debug("[mse-branding-cache] Redis 不可用，平台域品牌缓存重建跳过");
            return;
        }
        try {
            boolean executed = lockOps.tryExecute(REBUILD_LOCK_KEY, () -> {
                cache.clearPlatform();
                Map<String, DomainBrandingEntry> entries = new LinkedHashMap<>();
                List<SysDomainConfig> configs = domainConfigService.lambdaQuery()
                        .eq(SysDomainConfig::getStatus, 0)
                        .list();
                for (SysDomainConfig config : configs) {
                    entries.put(config.getDomain(), toEntry(config));
                }
                cache.putAllPlatform(entries);
                log.info("[mse-branding-cache] 平台域品牌缓存重建完成: 域名 {} 条", entries.size());
            });
            if (!executed) {
                log.info("[mse-branding-cache] 其他实例正在重建平台域品牌缓存，本实例跳过");
            }
        } catch (Exception ex) {
            // Redis 异常降级：登录页解析走 DB 兜底，不阻断启动
            log.warn("[mse-branding-cache] 平台域品牌缓存启动重建失败（读取侧 DB 兜底）: {}", ex.getMessage());
        }
    }

    /** 平台域条目：仅品牌字段，租户三要素为 null（域名未绑定租户） */
    public static DomainBrandingEntry toEntry(SysDomainConfig config) {
        DomainBrandingEntry entry = new DomainBrandingEntry();
        entry.setSiteTitle(config.getSiteTitle());
        entry.setSiteLogo(config.getSiteLogo());
        entry.setLoginBackground(config.getLoginBackground());
        entry.setBackgroundColor(config.getBackgroundColor());
        entry.setCopyright(config.getCopyright());
        return entry;
    }
}
