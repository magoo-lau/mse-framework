package vip.mseapp.core.annotation;

import org.apache.dubbo.config.annotation.Method;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跨模块服务引用（单体/微服务双模式的核心抽象）。
 *
 * <p>业务代码统一使用本注解注入 {@code mse-api} 中定义的接口，由 common-rpc 按部署形态装配：
 * <ul>
 *   <li>{@code mse.rpc.mode=local}：从 Spring 容器解析本地实现 Bean 直调（单体模式，无需 Nacos/Dubbo），
 *       {@link Local} 镜像 Spring {@code @Autowired} 的 required 语义，多实现按 primary 消歧；</li>
 *   <li>{@code mse.rpc.mode=dubbo}：编程式创建 Dubbo ReferenceConfig 代理远程调用（微服务模式），
 *       {@link Dubbo} <b>全量拷贝</b> Dubbo {@code @DubboReference} 属性，由映射层落到原生 Config。</li>
 * </ul>
 *
 * <p>外层只保留基本属性（无）——dubbo 调用参数一律在 {@link Dubbo} 嵌套注解声明
 * （历史扁平属性 version/group/check/timeout 已并入 {@code Dubbo}）。
 *
 * <p>{@link Local}/{@link Dubbo} 为框架镜像注解：Java 注解无继承机制、common-core 亦不可引用
 * Spring/Dubbo 运行时注解类，各模式语义由 common-rpc 处理器按对应嵌套注解显式实现。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface RpcReference {

    /** local（单体）模式配置：注入容错语义（镜像 Spring {@code @Autowired}） */
    Local local() default @Local;

    /** dubbo（微服务）模式配置：全量引用属性（镜像 Dubbo {@code @DubboReference}） */
    Dubbo dubbo() default @Dubbo;

    /**
     * local 模式配置（注入语义，镜像 Spring {@code @Autowired}）。
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Local {

        /** 依赖是否必需：false 时容器无实现则注入 null 不报错（可选依赖，默认 true 与既有行为一致） */
        boolean required() default true;
    }

    /**
     * dubbo 模式配置（引用语义，<b>全量拷贝</b> Dubbo 3.3 {@code @DubboReference} 属性）。
     *
     * <p>仅 {@code mse.rpc.mode=dubbo} 时生效，由 common-rpc 映射到原生 ReferenceConfig/MethodConfig。
     * 未设置约定：String 空串 / 数组空 / int -1 均表示未设置（沿用 Dubbo 缺省）；boolean 显式落
     * Config（与原生注解处理一致）。
     *
     * <p>相对 @DubboReference 跳过 12 个属性（无法在本层表达或无意义）：
     * <ul>
     *   <li>{@code interfaceClass}/{@code interfaceName}：契约即字段类型，避免双真源；</li>
     *   <li>{@code generic}/{@code injvm}/{@code application}/{@code services}：Dubbo 已 @Deprecated
     *       （泛化调用请直接用原生 ReferenceConfig API）；</li>
     *   <li>{@code module}/{@code consumer}/{@code monitor}/{@code registry}/{@code protocol}：
     *       Config id 引用——application/registry 由 DubboRuntimeConfigs 统一提供，其余 Config 级
     *       配置走 Nacos {@code dubbo.*} 属性；</li>
     *   <li>{@code id}：引用代理身份由框架自管（按 接口#version#group 缓存）。</li>
     * </ul>
     *
     * <p>两处<b>有意保留</b>仓内缺省（与 Dubbo 原注解不同，防全量行为回退）：
     * {@code timeout} 默认 5000（原 -1=继承 consumer 缺省 1000ms）、{@code check} 默认 false（原 true）。
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

        /** 服务版本（与 provider 端 {@code @RpcService.Dubbo} version 对应） */
        String version() default "";

        /** 服务分组（与 provider 端 group 对应） */
        String group() default "";

        /** 点对点直连地址（绕过注册中心，如 nacos:// 或 ip:port，多地址逗号分隔） */
        String url() default "";

        /** 客户端协议实现（netty 等） */
        String client() default "";

        /** 启动时检查 provider 是否可用（有意保留 false 缺省：不依赖提供者先启动） */
        boolean check() default false;

        /** 是否饿汉式初始化（get() 后即建立连接） */
        boolean init() default true;

        /** 是否懒连接（缺省 false；调用出错时重连） */
        boolean lazy() default false;

        /** 是否使用事件分发粘性连接 */
        boolean stubevent() default false;

        /** 断线重连间隔毫秒（默认 2000） */
        String reconnect() default "";

        /** 令粘性连接（同客户端始终同提供者，利于有状态服务） */
        boolean sticky() default false;

        /** 代理方式（jdk/javassist） */
        String proxy() default "";

        /** 消费端存根类全名 */
        String stub() default "";

        /** 集群容错策略（failover/failfast/failsafe/failback/forking/broadcast 等） */
        String cluster() default "";

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

        /** 是否异步调用 */
        boolean async() default false;

        /** 消费端最大并发调用数（-1 = 未设置） */
        int actives() default -1;

        /** 异步调用是否发送已完成标记 */
        boolean sent() default false;

        /** Mock 降级（false/true/default/类全名/return null 等） */
        String mock() default "";

        /** 参数校验（jsr303） */
        String validation() default "";

        /** 调用超时毫秒（有意保留 5000 缺省：Dubbo 原注解 -1 会继承 consumer 缺省 1000ms） */
        int timeout() default 5000;

        /** 接口级缓存（如 lru） */
        String cache() default "";

        /** 消费端过滤器（扩展名数组，追加到默认链） */
        String[] filter() default {};

        /** 引用侧监听器（扩展名数组） */
        String[] listener() default {};

        /** 自定义参数（{"key=value"} 形式） */
        String[] parameters() default {};

        /** 标签路由 tag */
        String tag() default "";

        /** 多提供者返回值合并策略（如 default，配合集群 available/failover） */
        String merger() default "";

        /** 方法级配置（Dubbo 原生 {@code @Method}，单方法覆盖超时/重试/回调等；配套 {@code @Argument} 参数级） */
        Method[] methods() default {};

        /** 应用级服务发现指定提供者应用名（配合服务发现路由） */
        String[] providedBy() default {};

        /** 提供者端口（服务发现直连场景，-1 = 未设置） */
        int providerPort() default -1;

        /** 提供者所在命名空间（服务发现场景） */
        String providerNamespace() default "";

        /** 引用可见范围（local/remote，缺省两者） */
        String scope() default "";

        /** 是否异步 refer（补齐多引用订阅耗时） */
        boolean referAsync() default false;

        /** 是否卸载集群相关职责（轻量客户端场景） */
        boolean unloadClusterRelated() default false;
    }
}
