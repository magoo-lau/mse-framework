package vip.mseapp.service.platform.config;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vip.mseapp.fs.api.condition.AbstractFsModeCondition;

/**
 * {@link FsAvailableCondition} 的取反条件（品牌素材本地兜底实现专用）。两侧显式互斥、不依赖注册顺序
 * （javac InnerClasses 不保序，勿改回 @ConditionalOnMissingBean 让位式——Fs 侧先让位即静默落本地）。
 */
public class FsUnavailableCondition extends AbstractFsModeCondition {

    public FsUnavailableCondition() {
        super("mse.tenant.branding-storage");
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !isFsAvailable(context);
    }
}
