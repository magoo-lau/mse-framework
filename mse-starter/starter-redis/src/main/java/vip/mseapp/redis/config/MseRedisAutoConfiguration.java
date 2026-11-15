package vip.mseapp.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.redis.TenantCache;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 基础设施。
 *
 * <p>设计要点：RedisTemplate 走 Spring Boot 默认的 Lettuce ConnectionFactory（与 Sa-Token 的
 * Redis DAO 是验证过的标准组合）；Redisson 仅作为纯客户端提供分布式锁等原生 API，
 * 不接管 ConnectionFactory——redisson-spring-boot-starter 的连接工厂与 Sa-Token 的
 * setStringAndKeepTTL 存在 TTL 语义冲突（PX 负数）。
 *
 * <p>必须在 Boot 的 RedisAutoConfiguration 之前装配：本类的模板是 RedisTemplate&lt;String, Object&gt;，
 * 若 Boot 默认的 RedisTemplate&lt;Object, Object&gt;（包名排序在前）先注册，本类的
 * {@code @ConditionalOnMissingBean} 会退让，按泛型注入 String,Object 模板的业务 Bean 将找不到可用实例。
 */
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@ConditionalOnClass(RedisTemplate.class)
@EnableConfigurationProperties(DataRedisProperties.class)
public class MseRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // Jackson 3：java.time 支持已内置于 databind，无需再注册 JavaTimeModule；
        // LaissezFaireSubTypeValidator 在 Jackson 3 改为模块私有，放行全部子类型用 BasicPolymorphicTypeValidator 表达；
        // 序列化格式含 @class 类型信息，与旧 Jackson 2 格式不兼容——升级窗口需清空 Redis（见 README 已知限制）
        JsonMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(v -> v.withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class).build(),
                        DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
        GenericJacksonJsonRedisSerializer valueSerializer = new GenericJacksonJsonRedisSerializer(mapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /** 复用 spring.data.redis 连接参数构建纯 Redisson 客户端（仅用于锁等原生 API） */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(DataRedisProperties properties) {
        Config config = new Config();
        String address = "redis://" + properties.getHost() + ":" + properties.getPort();
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.getDatabase())
                .setPassword(properties.getPassword());
        return Redisson.create(config);
    }

    /** 缓存 Key 租户前缀：t:{tenantId}:xxx，防止跨租户缓存串号 */
    public static String tenantKey(String key) {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? key : "t:" + tenantId + ":" + key;
    }

    /** 分布式锁操作封装（单体多实例/集群定时任务防重入） */
    public static class LockOps {

        private final RedissonClient redisson;

        public LockOps(RedissonClient redisson) {
            this.redisson = redisson;
        }

        /** 在锁保护下执行：获取不到锁立即失败 */
        public <T> T tryLockExecute(String lockKey, Supplier<T> action) {
            RLock lock = redisson.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 60, TimeUnit.SECONDS);
                if (!locked) {
                    throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "获取分布式锁失败: " + lockKey);
                }
                return action.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "获取分布式锁被中断");
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        /**
         * 抢锁执行（忙则跳过）：抢到锁执行并返回 true；锁被其他实例持有时不等待直接返回 false，
         * 调用方整体跳过本次执行（等待只会把 N 份工作串行化，跳过才是真省——多实例启动登记类
         * 一次性任务防重入）；Redis 异常向外抛，由调用方决定降级。
         */
        public boolean tryExecute(String lockKey, Runnable action) {
            RLock lock = redisson.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 60, TimeUnit.SECONDS);
                if (!locked) {
                    return false;
                }
                action.run();
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "获取分布式锁被中断");
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        /**
         * 窗口单飞标记：key 不存在则写入（ttl 到期自动释放）并返回 true（获得本窗口执行权），
         * 已存在返回 false。与 tryExecute 的区别：标记跨整个时间窗生效——锁只在执行期间持有，
         * 挡不住错峰重入，多实例周期推送/接收「同一窗口只处理一次」的收敛用本方法。
         */
        public boolean tryEnter(String key, Duration ttl) {
            RBucket<String> bucket = redisson.getBucket(key);
            return bucket.trySet("1", ttl.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public LockOps mseLockOps(RedissonClient redissonClient) {
        return new LockOps(redissonClient);
    }

    /**
     * 调度任务执行互斥（common-core TaskExecutionGuard 的 Redis 实现）：统一调度中心
     * local 模式的多实例防重——每轮先抢 Redisson 看门狗锁（持锁期间自动续期），
     * 抢不到即让位。Redis 故障时放行执行（可用性优先于防重，WARN 提示）。
     */
    public static class RedisTaskExecutionGuard implements vip.mseapp.core.schedule.TaskExecutionGuard {

        private static final org.slf4j.Logger guardLog =
                org.slf4j.LoggerFactory.getLogger(RedisTaskExecutionGuard.class);

        private static final String LOCK_PREFIX = "mse:lock:task:";

        private final RedissonClient redisson;

        public RedisTaskExecutionGuard(RedissonClient redisson) {
            this.redisson = redisson;
        }

        @Override
        public boolean tryRun(String taskId, Runnable action) {
            RLock lock = redisson.getLock(LOCK_PREFIX + taskId);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Throwable t) {
                guardLog.warn("[mse-schedule] 任务互斥锁不可用（Redis 异常，放行执行）: {} - {}", taskId, t.toString());
                action.run();
                return true;
            }
            if (!locked) {
                return false;
            }
            try {
                action.run();
                return true;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                    } catch (Throwable ignored) {
                        // 下线/续期边界的解锁失败无需处理（看门狗随连接关闭失效）
                    }
                }
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(vip.mseapp.core.schedule.TaskExecutionGuard.class)
    public RedisTaskExecutionGuard mseTaskExecutionGuard(RedissonClient redissonClient) {
        return new RedisTaskExecutionGuard(redissonClient);
    }

    /**
     * 权限/角色集合缓存（版本号 + TTL 失效）：基座本地鉴权数据源与 starter-bridge
     * 的 RPC 鉴权数据源共用，任一侧回填另一侧命中，版本失效对两端同时生效。
     */
    @Bean
    @ConditionalOnMissingBean(PermissionCache.class)
    public PermissionCache permissionCache(RedisTemplate<String, Object> redisTemplate) {
        return new PermissionCache(redisTemplate);
    }

    /** 租户权益快照缓存（0.1.0 权益模型：版本号 + TTL 双保险，范式同 PermissionCache） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.redis.EntitlementCache.class)
    public vip.mseapp.redis.EntitlementCache entitlementCache(RedisTemplate<String, Object> redisTemplate) {
        return new vip.mseapp.redis.EntitlementCache(redisTemplate);
    }

    /** 登录页域名个性化缓存（双 hash 按域名寻址）：service-system/service-platform 启动注册 + 变更写穿 */
    @Bean
    @ConditionalOnMissingBean(DomainBrandingCache.class)
    public DomainBrandingCache domainBrandingCache(RedisTemplate<String, Object> redisTemplate) {
        return new DomainBrandingCache(redisTemplate);
    }

    /**
     * 租户控制面缓存（租户×服务路由快照 + 租户行快照，版本号 + TTL 失效）：service-platform
     * 路由解析/租户查询与 tenant-starter standalone 本地桩共用，任一实例回填其余实例命中。
     */
    @Bean
    @ConditionalOnMissingBean(TenantCache.class)
    public TenantCache tenantCache(RedisTemplate<String, Object> redisTemplate) {
        return new TenantCache(redisTemplate);
    }
}
