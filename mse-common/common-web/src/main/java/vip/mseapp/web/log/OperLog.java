package vip.mseapp.web.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 写方法上，由 {@link OperLogAspect} 异步记录到操作日志表。
 *
 * <p>落库由 classpath 中的 {@link OperLogRecorder} 实现完成（当前 service-system 提供）；
 * 无实现时切面静默跳过，仅打印 debug 日志。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 业务模块（如 system:user / system:role），用于日志检索 */
    String module();

    /** 操作类型（新增/修改/删除/导入/导出等，直接入库展示） */
    String type() default "";
}
