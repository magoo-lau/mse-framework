package vip.mseapp.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务模块服务身份声明：标在模块的 ServiceAutoConfiguration 自动装配类上，
 * 声明本模块的"服务名"与 mapper 包根，框架启动时自动收集合并进租户路由的服务名解析。
 *
 * <p>解决的问题：单体聚合多业务模块时，「租户隔离策略」的服务级配置按服务名生效，
 * 框架需要知道"哪条 SQL 属于哪个服务"。此前依赖接入方在 yaml 配置
 * {@code mse.tenant.service-mappings.<服务名>=<mapper包前缀>}，每接入一个模块都要改
 * 接入方配置；本注解让模块**自带身份**——接入方加 pom 依赖即零配置生效。
 *
 * <p>使用约定：
 * <ul>
 *   <li>{@link #value()}：服务名，与微服务形态 {@code spring.application.name} 取同一值
 *       （如 mse-oa），保证同一模块双形态服务名一致；</li>
 *   <li>{@link #packages()}：mapper 包根（最长前缀匹配），缺省从注解类所在包推导——
 *       去掉末段 {@code .autoconfigure}（业务模块）或 {@code .config}（基座模块）；
 *       聚合工程（如 oa 的 form/bpm 子模块）各装配类标同一服务名即可，包根各自覆盖。</li>
 * </ul>
 *
 * <p>优先级：yaml {@code mse.tenant.service-mappings}（显式逃生舱）&gt; 本注解 &gt; 缺省服务名
 * （service-name → spring.application.name → '*'）。微服务形态整服务即一个模块，
 * 服务名即应用名，本注解冗余但无害；基座 service-* 模块不标注（平台管控表走缺省）。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantServiceModule {

    /** 服务名（与微服务 spring.application.name 同值，如 mse-oa；'未用 *'，隔离策略配置禁止 '*'） */
    String value();

    /** mapper 包根数组；缺省从注解类所在包去掉末段 .autoconfigure / .config 推导 */
    String[] packages() default {};
}
