package vip.mseapp.core.annotation;

import org.apache.dubbo.config.annotation.Method;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跨模块 RPC 契约实现标记（与 {@link RpcReference} 成对：{@code @RpcReference} 消费，{@code @RpcService} 提供）。
 *
 * <p>标注在契约实现类上，声明该实现以 {@code value} 指定的接口对外提供 RPC 服务。由 common-rpc 自动装配：
 * <ul>
 *   <li>{@code mse.rpc.mode=dubbo} 且 classpath 存在 Dubbo 时：实现 Bean 就绪后自动导出为 Dubbo 服务，
 *       无需在启动器手写 ServiceConfig 导出配置；{@link Dubbo} 携带 @DubboService 的全量导出属性；</li>
 *   <li>单体模式（默认 local，不引 Dubbo 依赖）：本注解自身不产生副作用，实现 Bean 仅作本地直调；
 *       {@link Local} 携带 bean 名/primary 消歧元数据，配合兜底自动注册
 *       （{@code mse.rpc.local.scan-packages}，契约在容器中无实现 Bean 时才兜底注册）使用。</li>
 * </ul>
 *
 * <p>业务工程微服务化时只需：实现类标 {@code @RpcService(契约.class)}，启动器引入
 * {@code dubbo-spring-boot-starter} 并配置 {@code mse.rpc.mode=dubbo} 与注册中心地址即可。
 *
 * <p>{@link Local}/{@link Dubbo} 为框架镜像注解（分别对应 Spring {@code @Service} 与 Dubbo
 * {@code @DubboService}）：Java 注解无继承机制、common-core 亦不可引用 Dubbo 类，
 * {@code Dubbo} 采取<b>全量拷贝</b> @DubboService 属性策略，各模式语义由 common-rpc 处理器显式实现。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {

    /** 对外提供的契约接口（必填，可实现多个契约） */
    Class<?>[] value();

    /** local（单体）模式配置：bean 名与 primary 消歧（镜像 Spring {@code @Service} 语义） */
    Local local() default @Local;

    /** dubbo（微服务）模式配置：全量导出属性（镜像 Dubbo {@code @DubboService}） */
    Dubbo dubbo() default @Dubbo;

    /**
     * local 模式配置（bean 注册语义，镜像 Spring {@code @Service} 子集 + primary 扩展）。
     *
     * <ul>
     *   <li>{@code value}：bean 名——兜底自动注册时生效；实现 Bean 已由模块 AutoConfiguration
     *       的 {@code @Bean} 方法注册时，以 {@code @Bean} 方法名为准；</li>
     *   <li>{@code primary}：多实现消歧——local 模式下同契约存在多个实现 Bean 时，
     *       恰一个 primary 的候选胜出（消费端 {@code @RpcReference} 解析时识别）。</li>
     * </ul>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Local {

        /** bean 名（缺省按实现类名生成，与 Spring 默认命名一致） */
        String value() default "";

        /** 多实现消歧标记（local 模式下同契约多 Bean 时优先选中；Spring 侧为独立 @Primary，此处并入） */
        boolean primary() default false;
    }

    /**
     * dubbo 模式配置（导出语义，<b>全量拷贝</b> Dubbo 3.3 {@code @DubboService} 属性）。
     *
     * <p>仅 {@code mse.rpc.mode=dubbo} 时生效，由导出处理器映射到原生 ServiceConfig/MethodConfig。
     * 未设置约定：String 空串 / 数组空 / int -1 均表示未设置（沿用 Dubbo 缺省）；boolean 显式落
     * Config（与原生注解处理一致）。
     *
     * <p>相对 @DubboService 跳过 9 个属性（无法在本层表达或无意义）：
     * <ul>
     *   <li>{@code interfaceClass}/{@code interfaceName}：契约由外层 {@code value()} 声明，避免双真源；</li>
     *   <li>{@code local}/{@code application}：Dubbo 已 @Deprecated；</li>
     *   <li>{@code module}/{@code provider}/{@code monitor}/{@code registry}/{@code protocol}：
     *       Config id 引用——application/registry 由 DubboRuntimeConfigs 统一提供，其余 Config 级
     *       配置走 Nacos {@code dubbo.*} 属性或手写 ServiceConfig。</li>
     * </ul>
     *
     * <p>{@code methods} 直接使用 Dubbo 原生 {@link Method @Method}/{@code @Argument} 注解（零漂移）。
     * 为此 common-core 以<b>编译期传递依赖</b>引入 dubbo 本体（运行件版本由基座 BOM/Spring Boot
     * netty 管理对齐）：javac 完成注解符号时解析全部属性签名，下游模块的编译类路径必须有 dubbo 类
     * （optional 不传递，实测全量编译失败；Spring 运行期反射完成属性签名同理）；单体运行期经
     * {@code mse.rpc.mode} 门控零触达（local 分支短路、核心 jar 无 Spring 自动装配注册）。
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Dubbo {

        /** 服务版本（与消费端 {@code @RpcReference.Dubbo} version 对应） */
        String version() default "";

        /** 服务分组（与消费端 group 对应） */
        String group() default "";

        /** 服务路径（缺省=接口全名） */
        String path() default "";

        /** 是否暴露服务 */
        boolean export() default true;

        /** 令牌鉴权（true=UUID 自动生成，或固定值） */
        String token() default "";

        /** 是否已废弃 */
        boolean deprecated() default false;

        /** 服务是否动态注册（false=注册后禁用提供者仍可调） */
        boolean dynamic() default true;

        /** 访问日志（true/缺省路径/日志文件名） */
        String accesslog() default "";

        /** 提供端最大并行执行请求数（-1 = 未设置） */
        int executes() default -1;

        /** 是否注册到注册中心 */
        boolean register() default true;

        /** 服务预热/权重调度权重（-1 = 未设置） */
        int weight() default -1;

        /** 服务文档 URL */
        String document() default "";

        /** 延迟注册毫秒（-1 = 未设置；-1 之外负数表示延迟基数，见 Dubbo 文档） */
        int delay() default -1;

        /** 服务端存根类全名 */
        String stub() default "";

        /** 集群容错策略（failover/failfast/failsafe/failback/forking/broadcast 等） */
        String cluster() default "";

        /** 代理方式（jdk/javassist） */
        String proxy() default "";

        /** 每客户端连接数（-1 = 未设置，连接型协议有意义） */
        int connections() default -1;

        /** 回调实例上限（-1 = 未设置） */
        int callbacks() default -1;

        /** 连接建立触发的方法名 */
        String onconnect() default "";

        /** 连接断开触发的方法名 */
        String ondisconnect() default "";

        /** 负责人（运维归属） */
        String owner() default "";

        /** 服务分层（如 biz/data） */
        String layer() default "";

        /** 失败重试次数（-1 = 未设置；建议只对幂等方法设置） */
        int retries() default -1;

        /** 负载均衡策略（roundrobin/random/leastactive/consistenthash 等） */
        String loadbalance() default "";

        /** 是否异步执行 */
        boolean async() default false;

        /** 消费端最大并发调用数（-1 = 未设置） */
        int actives() default -1;

        /** 异步调用是否发送已完成标记 */
        boolean sent() default false;

        /** Mock 降级（false/true/default/类全名/return null 等） */
        String mock() default "";

        /** 参数校验（jsr303） */
        String validation() default "";

        /** 接口级超时毫秒（-1 = 未设置，继承全局 provider 缺省） */
        int timeout() default -1;

        /** 接口级缓存（如 lru） */
        String cache() default "";

        /** 提供端过滤器（扩展名数组，追加到默认链） */
        String[] filter() default {};

        /** 导出侧监听器（扩展名数组） */
        String[] listener() default {};

        /** 自定义参数（{"key=value"} 形式） */
        String[] parameters() default {};

        /** 标签路由 tag */
        String tag() default "";

        /** 方法级配置（Dubbo 原生 {@code @Method}，单方法覆盖超时/重试/回调等；配套 {@code @Argument} 参数级） */
        Method[] methods() default {};

        /** 服务可见范围（local/remote，缺省两者） */
        String scope() default "";

        /** 是否异步导出（补齐多协议导出耗时） */
        boolean exportAsync() default false;

        /** 业务线程池 Bean 名 */
        String executor() default "";

        /** 单请求负载上限（字节数或带单位字符串） */
        String payload() default "";

        /** 序列化方式（fastjson2/hessian2 等） */
        String serialization() default "";

        /** 优选序列化协商列表（逗号分隔，如 fastjson2,hessian2） */
        String preferSerialization() default "";
    }
}
