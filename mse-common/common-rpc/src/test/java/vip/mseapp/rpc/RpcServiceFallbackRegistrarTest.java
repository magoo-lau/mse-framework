package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.rpc.fixtures.scan.ScanContract;
import vip.mseapp.rpc.fixtures.scan.ScanContractImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RpcServiceFallbackRegistrar} 兜底自动注册：无实现 Bean 时兜底注册；
 * 已有 @Bean 实现时让位（SPI 替换语义永远优先）；未配置 scan-packages 时零行为。
 */
class RpcServiceFallbackRegistrarTest {

    private AnnotationConfigApplicationContext ctx;

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.close();
        }
    }

    static class FallbackConsumer {
        @RpcReference
        ScanContract svc;
    }

    @Configuration
    static class ManualConfig {
        @Bean
        ScanContract manualScanContract() {
            return new ManualImpl();
        }
    }

    static class ManualImpl implements ScanContract {
        @Override
        public String name() {
            return "manual";
        }
    }

    private AnnotationConfigApplicationContext contextWithScan(boolean withScan, boolean withConsumer) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (withScan) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of(RpcServiceFallbackRegistrar.SCAN_PACKAGES_PROPERTY, "vip.mseapp.rpc.fixtures.scan")));
        }
        context.registerBean(RpcServiceFallbackRegistrar.class);
        context.registerBean(RpcReferenceBeanPostProcessor.class, () -> new RpcReferenceBeanPostProcessor("local"));
        if (withConsumer) {
            context.registerBean(FallbackConsumer.class);
        }
        return context;
    }

    @Test
    void fallbackRegistersWhenContractUncovered() {
        ctx = contextWithScan(true, true);
        ctx.refresh();
        // local().value 显式 bean 名生效；同契约第二候选（默认名路径）被让位跳过
        assertTrue(ctx.containsBean("scanFallbackImpl"), "应按 local().value 兜底注册");
        assertFalse(ctx.containsBean("scanContractImpl2"), "同契约后续候选应让位");
        assertEquals(1, ctx.getBeansOfType(ScanContract.class).size());
        FallbackConsumer consumer = ctx.getBean(FallbackConsumer.class);
        assertInstanceOf(ScanContractImpl.class, consumer.svc);
        assertEquals("scan-impl", consumer.svc.name());
    }

    @Test
    void manualBeanWinsOverFallback() {
        ctx = contextWithScan(true, true);
        ctx.register(ManualConfig.class);
        ctx.refresh();
        assertEquals(1, ctx.getBeansOfType(ScanContract.class).size());
        assertEquals("manual", ctx.getBean(FallbackConsumer.class).svc.name());
        assertFalse(ctx.containsBean("scanFallbackImpl"), "已有实现 Bean 时不应兜底注册");
    }

    @Test
    void disabledWithoutScanPackagesProperty() {
        // 未配置 scan-packages：功能完全关闭，不注册任何兜底 bean
        ctx = contextWithScan(false, false);
        ctx.refresh();
        assertEquals(0, ctx.getBeansOfType(ScanContract.class).size());
        assertFalse(ctx.containsBean("scanFallbackImpl"));
    }
}
