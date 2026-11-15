package vip.mseapp.boot.gateway.config;

import java.util.List;

/**
 * 网关鉴权白名单持有者（mse.gateway.ignore-urls，配置驱动）。
 *
 * <p>ignore-urls 为免会话校验路径（Ant 风格）：放行的依据是「该路径自带等价或更强的鉴权」——
 * 公开登录端点、机器凭证端点（/open/oauth/token、/open/api/** 由 open-service 应用凭证链路校验）、
 * 第三方回调（/mp/callback/** 由下游验签）。细粒度权限仍由下游 @SaCheckPermission 把守（纵深防御）。
 *
 * <p>⚠️ 刻意不是 @ConfigurationProperties：本仓库网关的 spring-cloud refresh 管道不回放
 * Nacos 配置（Refresh keys 恒为空），ConfigurationPropertiesRebinder 会用陈旧环境把
 * 监听器已热更新的值重绑回旧值。故启动值由 SaTokenGatewayConfig 用 Binder 从环境读一次，
 * 运行期仅由 Nacos 客户端监听器更新本对象（volatile，写读跨线程）。
 *
 * <p>⚠️ list 为整体替换语义：在 Nacos 配置本项时须全量列出（含默认项）。
 */
public class GatewayAuthProperties {

    /** 免会话校验路径（Ant 风格，如 /open/api/**） */
    private volatile List<String> ignoreUrls = defaultIgnoreUrls();

    private static List<String> defaultIgnoreUrls() {
        return List.of(
                "/auth/login",
                "/auth/platform/login",
                "/auth/tenant/resolve",
                "/auth/domain/config",
                "/actuator/**",
                "/error");
    }

    public List<String> getIgnoreUrls() {
        return ignoreUrls;
    }

    public void setIgnoreUrls(List<String> ignoreUrls) {
        this.ignoreUrls = ignoreUrls;
    }
}
