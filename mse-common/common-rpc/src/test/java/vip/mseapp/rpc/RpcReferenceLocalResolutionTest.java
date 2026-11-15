package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.rpc.fixtures.RemoteContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * local 模式消费端语义：{@code @RpcReference.local()}（required）与多实现 primary 消歧。
 */
class RpcReferenceLocalResolutionTest {

    static class RequiredConsumer {
        @RpcReference
        RemoteContract svc;
    }

    static class OptionalConsumer {
        @RpcReference(local = @RpcReference.Local(required = false))
        RemoteContract svc;
    }

    @RpcService(value = RemoteContract.class, local = @RpcService.Local(primary = true))
    static class PrimaryImpl implements RemoteContract {
        @Override
        public String name() {
            return "primary";
        }
    }

    @RpcService(RemoteContract.class)
    static class SecondaryImpl implements RemoteContract {
        @Override
        public String name() {
            return "secondary";
        }
    }

    static class PlainImpl implements RemoteContract {
        @Override
        public String name() {
            return "plain";
        }
    }

    @Configuration
    static class TwoImplConfig {
        @Bean
        PrimaryImpl primaryImpl() {
            return new PrimaryImpl();
        }

        @Bean
        SecondaryImpl secondaryImpl() {
            return new SecondaryImpl();
        }
    }

    @Configuration
    static class TwoImplNoPrimaryConfig {
        @Bean
        PlainImpl plainA() {
            return new PlainImpl();
        }

        @Bean
        PlainImpl plainB() {
            return new PlainImpl();
        }
    }

    private AnnotationConfigApplicationContext context(Class<?>... configs) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(RpcReferenceBeanPostProcessor.class, () -> new RpcReferenceBeanPostProcessor("local"));
        if (configs.length > 0) {
            ctx.register(configs);
        }
        ctx.registerBean(RequiredConsumer.class);
        return ctx;
    }

    @Test
    void requiredTrueWithoutImplFailsFast() {
        AnnotationConfigApplicationContext ctx = context();
        BeanCreationException ex = assertThrows(BeanCreationException.class, ctx::refresh);
        Throwable root = rootCause(ex);
        assertInstanceOf(IllegalStateException.class, root);
        assertTrue(root.getMessage().contains("找不到"), root.getMessage());
    }

    @Test
    void requiredFalseWithoutImplInjectsNull() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(RpcReferenceBeanPostProcessor.class, () -> new RpcReferenceBeanPostProcessor("local"));
        ctx.registerBean(OptionalConsumer.class);
        ctx.refresh();
        assertNull(ctx.getBean(OptionalConsumer.class).svc);
    }

    @Test
    void singleImplInjectedByType() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(RpcReferenceBeanPostProcessor.class, () -> new RpcReferenceBeanPostProcessor("local"));
        ctx.registerBean("plainImpl", PlainImpl.class, PlainImpl::new);
        ctx.registerBean(RequiredConsumer.class);
        ctx.refresh();
        RequiredConsumer consumer = ctx.getBean(RequiredConsumer.class);
        assertNotNull(consumer.svc);
        assertEquals("plain", consumer.svc.name());
    }

    @Test
    void multipleImplsWithRpcServicePrimaryResolvesWinner() {
        AnnotationConfigApplicationContext ctx = context(TwoImplConfig.class);
        ctx.refresh();
        assertEquals("primary", ctx.getBean(RequiredConsumer.class).svc.name());
    }

    @Test
    void multipleImplsWithoutPrimaryStillFails() {
        AnnotationConfigApplicationContext ctx = context(TwoImplNoPrimaryConfig.class);
        BeanCreationException ex = assertThrows(BeanCreationException.class, ctx::refresh);
        Throwable root = rootCause(ex);
        assertInstanceOf(IllegalStateException.class, root);
        assertTrue(root.getMessage().contains("存在多个实现"), root.getMessage());
    }

    @Test
    void multipleImplsWithBeanDefinitionPrimaryResolvesWinner() {
        // bean 定义 primary（非 @RpcService.local().primary()）同样参与消歧
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(RpcReferenceBeanPostProcessor.class, () -> new RpcReferenceBeanPostProcessor("local"));
        BeanDefinition primaryDef = BeanDefinitionBuilder.genericBeanDefinition(PlainImpl.class).getBeanDefinition();
        primaryDef.setPrimary(true);
        ((org.springframework.beans.factory.support.BeanDefinitionRegistry) ctx.getBeanFactory())
                .registerBeanDefinition("primaryPlain", primaryDef);
        ctx.registerBean("plainB", PlainImpl.class, PlainImpl::new);
        ctx.registerBean(RequiredConsumer.class);
        ctx.refresh();
        assertEquals("plain", ctx.getBean(RequiredConsumer.class).svc.name());
        assertTrue(ctx.getBeanFactory().getBeanDefinition("primaryPlain").isPrimary());
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
