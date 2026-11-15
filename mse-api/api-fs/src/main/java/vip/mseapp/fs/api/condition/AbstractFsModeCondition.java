package vip.mseapp.fs.api.condition;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import vip.mseapp.fs.api.RemoteFileService;

/**
 * mse-fs 可用性条件基类（各存储消费模块条件对的公共五步判定，REGISTER_BEAN 阶段求值、
 * 以 afterName 保证在 FsServiceAutoConfiguration 之后评估）：
 * <ul>
 *   <li>模块自有 {@code <propertyKey>=local} → 恒 false（强制本地磁盘）；</li>
 *   <li>classpath 无 fs 契约（未引入 mse-fs）→ false，降级本地；</li>
 *   <li>auto（默认）且 {@code mse.rpc.mode=dubbo}（微服务形态）→ true，经 dubbo 调 mse-fs；</li>
 *   <li>auto 且 local 模式（单体）→ 需容器已装配 fs 契约实现（聚合了 fs-service）才 true。</li>
 * </ul>
 * 显式 {@code =fs} 时只要契约在 classpath 即 true（强制走 mse-fs，未部署在调用期报错）。
 *
 * <p>用法：各模块在自己的 autoconfigure 包保留同名薄子类（构造传配置键），Available 子类
 * 返回 {@link #isFsAvailable}、Unavailable/Local 子类取反——两侧显式互斥，不依赖注册顺序
 * （javac InnerClasses 不保序，勿改回 @ConditionalOnMissingBean 让位式）。
 * {@code @Conditional} 使用点引用子类类名，基类演进不影响装配声明。
 */
public abstract class AbstractFsModeCondition implements ConfigurationCondition {

    private final String propertyKey;

    protected AbstractFsModeCondition(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    /** fs 可用性五步判定（模块语义说明见各子类 javadoc） */
    protected boolean isFsAvailable(ConditionContext context) {
        Environment env = context.getEnvironment();
        String mode = env.getProperty(propertyKey, "auto");
        if ("local".equalsIgnoreCase(mode)) {
            return false;
        }
        if (!ClassUtils.isPresent("vip.mseapp.fs.api.RemoteFileService", getClass().getClassLoader())) {
            return false;
        }
        if ("fs".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("dubbo".equalsIgnoreCase(env.getProperty("mse.rpc.mode", "local"))) {
            return true;
        }
        // 单体 local 模式：仅当 fs-service 已在容器注册契约实现（避免 @RpcReference 本地解析启动失败）
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        return beanFactory != null
                && beanFactory.getBeanNamesForType(RemoteFileService.class, true, false).length > 0;
    }
}
