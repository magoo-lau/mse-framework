package vip.mseapp.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mseapp.core.exception.ServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link TenantContext} 核心语义：runAs 嵌套与恢复（含异常路径）、runIgnore 恢复、
 * runAsPlatform=租户 0、上下文缺失快速失败。这些是四世界路由的 ThreadLocal 基座，
 * 任何恢复泄漏都会表现为跨租户串数据。
 */
class TenantContextTest {

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    @Test
    void runAsRestoresPreviousValue() {
        TenantContext.setTenantId(1L);
        TenantContext.runAs(200L, () -> assertThat(TenantContext.getTenantId()).isEqualTo(200L));
        assertThat(TenantContext.getTenantId()).as("runAs 结束恢复外层租户").isEqualTo(1L);
    }

    @Test
    void runAsRestoresNullAfterNoContext() {
        TenantContext.runAs(200L, () -> assertThat(TenantContext.getTenantId()).isEqualTo(200L));
        assertThat(TenantContext.getTenantId()).as("外层无上下文时恢复为 null（remove 而非 set null）").isNull();
    }

    @Test
    void runAsRestoresOnException() {
        TenantContext.setTenantId(1L);
        assertThrows(IllegalStateException.class, () ->
                TenantContext.runAs(300L, () -> {
                    throw new IllegalStateException("boom");
                }));
        assertThat(TenantContext.getTenantId()).as("异常路径同样恢复").isEqualTo(1L);
    }

    @Test
    void nestedRunAsUnwindsInOrder() {
        TenantContext.setTenantId(1L);
        TenantContext.runAs(2L, () -> {
            TenantContext.runAs(3L, () -> assertThat(TenantContext.getTenantId()).isEqualTo(3L));
            assertThat(TenantContext.getTenantId()).as("内层退出后回到中层").isEqualTo(2L);
        });
        assertThat(TenantContext.getTenantId()).isEqualTo(1L);
    }

    @Test
    void runIgnoreRestoresPreviousFlag() {
        TenantContext.runAs(1L, () -> {
            assertThat(TenantContext.isIgnoreTenant()).isFalse();
            TenantContext.runIgnore(() -> assertThat(TenantContext.isIgnoreTenant()).isTrue());
            assertThat(TenantContext.isIgnoreTenant()).as("runIgnore 结束恢复").isFalse();
            assertThrows(IllegalStateException.class, () ->
                    TenantContext.runIgnore((Runnable) () -> {
                        throw new IllegalStateException("boom");
                    }));
            assertThat(TenantContext.isIgnoreTenant()).as("runIgnore 异常路径恢复").isFalse();
        });
    }

    @Test
    void runAsPlatformIsTenantZero() {
        TenantContext.runAsPlatform(() -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(0L);
            assertThat(TenantContext.isPlatform()).isTrue();
        });
    }

    @Test
    void clearResetsEverything() {
        TenantContext.setTenantId(1L);
        TenantContext.setIgnoreTenant(true);
        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(TenantContext.isIgnoreTenant()).isFalse();
    }

    @Test
    void requiredTenantIdFailsFastWhenMissing() {
        ServiceException error = assertThrows(ServiceException.class, TenantContext::getRequiredTenantId);
        assertThat(error.getMessage()).as("上下文缺失必须快速失败而非静默全表").contains("租户");
    }
}
