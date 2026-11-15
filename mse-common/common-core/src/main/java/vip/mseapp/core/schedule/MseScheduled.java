package vip.mseapp.core.schedule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内置后台任务声明（{@code @Scheduled} 的调度器无关版）：标注在任意 Spring Bean 的方法上，
 * 由装配件的注解收集器统一注册到当前 {@link MseTaskScheduler} 实现。
 *
 * <p>与 {@code @Scheduled} 的差异：
 * <ul>
 *   <li>调度由统一调度中心接管——应用就绪 + {@code mse.scheduling.startup-delay} 后才开始
 *       （避免先于结构迁移/依赖就绪执行），多实例经分布式锁防重；</li>
 *   <li>taskId 全局唯一，进管理面（平台端「任务调度」启停/触发/状态可见）；</li>
 *   <li>换接 xxl-job 等外部调度器时任务代码零改动（实现同一 SPI 即可整体切换）。</li>
 * </ul>
 *
 * <p>属性语义对齐 {@code @Scheduled}：fixedDelay/fixedDelayString、initialDelay/initialDelayString、
 * cron（均支持 {@code ${...}}} 占位符），另加 startup（启动一次性任务）。
 * 注解方法必须无参（public 或包级可见均可）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MseScheduled {

    /** 任务编号（全局唯一，管理面主键；惯例用现有线程名或语义名，如 mes-andon-escalate） */
    String taskId();

    /** 展示名（缺省同 taskId） */
    String name() default "";

    /** 描述（管理面列表展示） */
    String description() default "";

    /** 固定间隔毫秒（上轮结束到下轮开始），与 fixedDelayString 二选一 */
    long fixedDelay() default 0;

    /** 固定间隔文本（毫秒数，支持 ${...} 占位符），与 fixedDelay 二选一 */
    String fixedDelayString() default "";

    /** 首轮延迟毫秒（相对统一延迟启动窗口） */
    long initialDelay() default 0;

    /** 首轮延迟文本（毫秒数，支持 ${...} 占位符） */
    String initialDelayString() default "";

    /** cron 表达式（支持 ${...} 占位符），与 fixedDelay 二选一 */
    String cron() default "";

    /** 是否参与多实例防重（缺省 true=每轮抢分布式锁恰一实例执行；false=各实例独立执行） */
    boolean deduplicate() default true;

    /** 启动一次性任务：应用就绪 + 统一延迟启动窗口后执行一次（忽略 fixedDelay/cron） */
    boolean startup() default false;

    /**
     * 任务分类（管理面分域展示）：platform=系统任务（控制面：目录扇出/权益对账/基座与自注册推送等，
     * 平台运维全域对象）/ biz=业务域任务（缺省，扫描/重试/采集等业务基础设施）。
     * 取值对齐 {@link TaskDefinition#CATEGORY_PLATFORM}/{@link TaskDefinition#CATEGORY_BIZ}。
     */
    String category() default "biz";
}
