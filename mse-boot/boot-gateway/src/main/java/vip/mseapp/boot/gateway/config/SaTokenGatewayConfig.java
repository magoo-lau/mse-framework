package vip.mseapp.boot.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpLogic;
import com.alibaba.cloud.nacos.NacosConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;
import vip.mseapp.core.constant.MseConstants;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关统一会话校验：平台端 / 租户端任一账号体系登录即可放行；
 * 细粒度权限由下游服务的 @SaCheckPermission 注解校验（纵深防御）。
 *
 * <p>白名单配置驱动（mse.gateway.ignore-urls，见 {@link GatewayAuthProperties}）：
 * 在 setAuth 内逐请求匹配而非 addExclude 预编译——WebFlux 的 filter 链在启动期一次性构建，
 * 运行期变更必须落在逐请求读取的属性上。启动值从环境读取（Nacos 配置随启动生效）；
 * 运行期变更由 Nacos 客户端监听器直接更新（不走 spring-cloud refresh 管道，
 * 见 {@link GatewayAuthProperties} 类注释）。
 *
 * <p>阻断方式必须为抛出异常（checkLogin）——SaRouter.stop() 只停止匹配链、不拦截请求。
 */
@Configuration
public class SaTokenGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(SaTokenGatewayConfig.class);

    /** 网关独立持有两套 StpLogic（构造即注册），不依赖 common-security 的 Servlet 栈 */
    private static final StpLogic TENANT_LOGIC = new StpLogic(MseConstants.LOGIN_TYPE_TENANT);
    private static final StpLogic PLATFORM_LOGIC = new StpLogic(MseConstants.LOGIN_TYPE_PLATFORM);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Bean
    public GatewayAuthProperties gatewayAuthProperties(Environment environment) {
        GatewayAuthProperties properties = new GatewayAuthProperties();
        // 启动值：从环境（含 Nacos 配置）绑定，缺省用内置默认白名单
        List<String> configured = Binder.get(environment)
                .bind("mse.gateway.ignore-urls", Bindable.listOf(String.class))
                .orElse(null);
        if (configured != null && !configured.isEmpty()) {
            properties.setIgnoreUrls(configured);
        }
        return properties;
    }

    @Bean
    public SaReactorFilter saReactorFilter(GatewayAuthProperties properties,
                                           ObjectProvider<NacosConfigManager> configManager,
                                           Environment environment) {
        registerIgnoreUrlsListener(properties, configManager.getIfAvailable(), environment);
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    for (String pattern : properties.getIgnoreUrls()) {
                        if (PATH_MATCHER.match(pattern, path)) {
                            return;
                        }
                    }
                    if (!TENANT_LOGIC.isLogin() && !PLATFORM_LOGIC.isLogin()) {
                        // 抛 NotLoginException → setError 统一返回 401（提示按未登录原因细分）
                        TENANT_LOGIC.checkLogin();
                    }
                })
                .setError(SaTokenGatewayConfig::unauthorizedBody);
    }

    /** 401 响应体：按未登录原因细分提示（被强制下线/被顶下线/已过期），与 common-security 的 Servlet 侧 Advice 口径一致 */
    private static String unauthorizedBody(Throwable e) {
        String message = "账号未登录或会话已过期";
        if (e instanceof cn.dev33.satoken.exception.NotLoginException nle) {
            if (cn.dev33.satoken.exception.NotLoginException.KICK_OUT.equals(nle.getType())) {
                message = "您的账号已被强制下线，请重新登录";
            } else if (cn.dev33.satoken.exception.NotLoginException.BE_REPLACED.equals(nle.getType())) {
                message = "您的账号已在其他设备登录，如非本人操作请及时修改密码";
            } else if (cn.dev33.satoken.exception.NotLoginException.TOKEN_TIMEOUT.equals(nle.getType())) {
                message = "登录已过期，请重新登录";
            }
        }
        return "{\"code\":401,\"message\":\"" + message + "\",\"data\":null,\"success\":false}";
    }

    /**
     * 监听网关专属配置（Data ID = 应用名.yaml）：推送即解析 mse.gateway.ignore-urls 并更新白名单，
     * 运行期生效、无需重启。解析失败保留原白名单（绝不因配置笔误放行全部请求）。
     */
    private void registerIgnoreUrlsListener(GatewayAuthProperties properties,
                                            NacosConfigManager configManager, Environment environment) {
        if (configManager == null) {
            log.warn("[mse-gateway] Nacos ConfigManager 不可用，白名单仅保留启动值");
            return;
        }
        String dataId = environment.getRequiredProperty("spring.application.name") + ".yaml";
        String group = configManager.getNacosConfigProperties() != null
                && configManager.getNacosConfigProperties().getGroup() != null
                ? configManager.getNacosConfigProperties().getGroup() : "DEFAULT_GROUP";
        try {
            configManager.getConfigService().addListener(dataId, group, new com.alibaba.nacos.api.config.listener.AbstractListener() {
                @Override
                @SuppressWarnings("unchecked")
                public void receiveConfigInfo(String content) {
                    try {
                        Map<String, Object> root = new Yaml().load(content);
                        Object gateway = root == null ? null : ((Map<String, Object>) root.get("mse")).get("gateway");
                        Object urls = gateway == null ? null : ((Map<String, Object>) gateway).get("ignore-urls");
                        if (!(urls instanceof List<?> list)) {
                            log.warn("[mse-gateway] 白名单推送内容缺少 mse.gateway.ignore-urls，忽略本次变更");
                            return;
                        }
                        List<String> parsed = list.stream().map(String::valueOf).collect(Collectors.toList());
                        properties.setIgnoreUrls(parsed);
                        log.info("[mse-gateway] 白名单已热更新（{} 条）：{}", parsed.size(), parsed);
                    } catch (Exception e) {
                        log.warn("[mse-gateway] 白名单推送内容解析失败，保留原白名单", e);
                    }
                }
            });
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            log.warn("[mse-gateway] 白名单监听器注册失败，运行期变更需重启网关生效", e);
        }
    }
}
