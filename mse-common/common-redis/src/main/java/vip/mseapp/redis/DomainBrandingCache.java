package vip.mseapp.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * 登录页域名个性化缓存（Redis 双 hash，按域名寻址，启动全量注册 + 变更写穿）。
 *
 * <p>双 hash 分世界维护，互不覆盖：{@code mse:branding:domain:tenant}（域名绑定了租户的
 * 条目，service-system 启动重建 + 租户侧品牌/绑定变更写穿）与 {@code mse:branding:domain:platform}
 * （平台自有域名条目，service-platform 启动重建 + 管理端变更写穿）。读取侧先租户域后平台域
 * ——租户绑定的域名恒优先于平台自有配置（遗留的平台侧租户域名行自然被租户自助绑定遮蔽）。
 *
 * <p>缓存非持久真相：hash 被清（Redis 重启等）后读取侧按 key 惰性回填 DB 真相，不阻断登录。
 */
public class DomainBrandingCache {

    /** 租户域 hash：域名绑定了租户（tenantId &gt; 0），service-system 维护 */
    public static final String KEY_TENANT = "mse:branding:domain:tenant";

    /** 平台域 hash：平台自有域名（sys_domain_config），service-platform 维护 */
    public static final String KEY_PLATFORM = "mse:branding:domain:platform";

    private final RedisTemplate<String, Object> redisTemplate;

    public DomainBrandingCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 写入租户域条目（域名须小写规范化） */
    public void putTenantDomain(String domain, DomainBrandingEntry entry) {
        redisTemplate.<String, Object>opsForHash().put(KEY_TENANT, domain, entry);
    }

    /** 写入平台域条目（域名须小写规范化） */
    public void putPlatformDomain(String domain, DomainBrandingEntry entry) {
        redisTemplate.<String, Object>opsForHash().put(KEY_PLATFORM, domain, entry);
    }

    /** 读租户域条目；未命中返回 null */
    public DomainBrandingEntry getTenantDomain(String domain) {
        return read(KEY_TENANT, domain);
    }

    /** 读平台域条目；未命中返回 null */
    public DomainBrandingEntry getPlatformDomain(String domain) {
        return read(KEY_PLATFORM, domain);
    }

    /**
     * 摘除租户域条目（仅当条目确属该租户，防误删后绑定者的条目）；不属该租户则不动。
     */
    public void evictTenantDomain(String domain, long ownerTenantId) {
        DomainBrandingEntry existing = read(KEY_TENANT, domain);
        if (existing != null && existing.getTenantId() != null && existing.getTenantId() == ownerTenantId) {
            redisTemplate.<String, Object>opsForHash().delete(KEY_TENANT, domain);
        }
    }

    /** 摘除平台域条目 */
    public void evictPlatformDomain(String domain) {
        redisTemplate.<String, Object>opsForHash().delete(KEY_PLATFORM, domain);
    }

    /** 全量重建租户域：先清后写（仅启动 Runner 调用，运行期写穿禁止清库式重建） */
    public void clearTenant() {
        redisTemplate.delete(KEY_TENANT);
    }

    /** 全量重建平台域：先清后写（仅启动 Runner 调用） */
    public void clearPlatform() {
        redisTemplate.delete(KEY_PLATFORM);
    }

    /** 租户域 hash 是否已建立（惰性自愈的触发判据） */
    public boolean hasTenantHash() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_TENANT));
    }

    /** 平台域 hash 是否已建立 */
    public boolean hasPlatformHash() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PLATFORM));
    }

    /** 批量写入（重建用） */
    public void putAllTenant(Map<String, DomainBrandingEntry> entries) {
        if (!entries.isEmpty()) {
            redisTemplate.<String, Object>opsForHash().putAll(KEY_TENANT, entries);
        }
    }

    /** 批量写入（重建用） */
    public void putAllPlatform(Map<String, DomainBrandingEntry> entries) {
        if (!entries.isEmpty()) {
            redisTemplate.<String, Object>opsForHash().putAll(KEY_PLATFORM, entries);
        }
    }

    private DomainBrandingEntry read(String key, String domain) {
        Object value = redisTemplate.<String, Object>opsForHash().get(key, domain);
        return value instanceof DomainBrandingEntry entry ? entry : null;
    }
}
