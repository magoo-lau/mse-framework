package vip.mseapp.service.platform.config;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import vip.mseapp.fs.api.condition.AbstractFsModeCondition;

/**
 * mse-fs 可用性条件（平台品牌素材存储自动降级判据；判定五步与配置键语义见基类 {@link AbstractFsModeCondition}）。
 */
public class FsAvailableCondition extends AbstractFsModeCondition {

    public FsAvailableCondition() {
        super("mse.tenant.branding-storage");
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isFsAvailable(context);
    }
}
