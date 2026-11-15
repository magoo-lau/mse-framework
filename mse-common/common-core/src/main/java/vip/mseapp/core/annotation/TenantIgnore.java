package vip.mseapp.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法/类级“跳过租户隔离”声明（由 common-web 的切面实现）。
 *
 * <p>仅限平台运营端管理接口与定时任务等跨租户场景使用，业务接口严禁标注。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TenantIgnore {
}
