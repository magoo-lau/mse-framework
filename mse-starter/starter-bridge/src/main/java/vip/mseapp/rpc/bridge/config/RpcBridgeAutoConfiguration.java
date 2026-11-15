package vip.mseapp.rpc.bridge.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.mybatis.datasource.TenantDsMetadataProvider;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.rpc.bridge.RpcOperLogRecorder;
import vip.mseapp.rpc.bridge.RpcSaPermission;
import vip.mseapp.rpc.bridge.RpcTenantDsMetadataProvider;
import vip.mseapp.rpc.bridge.RpcTenantRouteResolver;
import vip.mseapp.web.log.OperLogRecorder;

/**
 * 基座契约消费端桥接自动装配（Spring Boot SPI）。
 *
 * <p>为未装配基座 service-* 的业务微服务提供 RPC 回退实现：
 * <ul>
 *   <li>{@link RpcSaPermission}——Sa-Token 鉴权数据源（@SaCheckPermission 的数据来源），消费 mse-system；</li>
 *   <li>{@link RpcTenantRouteResolver}——租户×服务路由解析（多模式隔离），消费 mse-tenant；</li>
 *   <li>{@link RpcTenantDsMetadataProvider}——租户数据源元数据 RPC 通道（运行时注册/回查），消费 mse-tenant；</li>
 *   <li>{@link RpcOperLogRecorder}——@OperLog 操作日志落库通道，消费 mse-system。</li>
 * </ul>
 *
 * <p><b>让位语义</b>：每个 Bean 同时带 {@code @ConditionalOnMissingBean(契约类型)} 与
 * {@code @ConditionalOnMissingClass(基座本地实现类名)}——单体聚合等引入了基座 service-* 的
 * 部署形态下，基座本地实现优先，本模块自动退让（依赖方向不反转：让位判断走类名字符串）。
 */
@AutoConfiguration
public class RpcBridgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    @ConditionalOnMissingClass("vip.mseapp.service.system.security.SaPermissionImpl")
    public RpcSaPermission rpcSaPermission(PermissionCache permissionCache) {
        return new RpcSaPermission(permissionCache);
    }

    @Bean
    @ConditionalOnMissingBean(TenantRouteResolver.class)
    @ConditionalOnMissingClass("vip.mseapp.service.platform.service.impl.SysTenantRouteServiceImpl")
    public RpcTenantRouteResolver rpcTenantRouteResolver(
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.rpc.bridge.TenantSnapshotStore> snapshotStoreProvider) {
        return new RpcTenantRouteResolver(snapshotStoreProvider);
    }

    @Bean
    @ConditionalOnMissingBean(TenantDsMetadataProvider.class)
    @ConditionalOnMissingClass("vip.mseapp.service.platform.service.impl.SysTenantRouteServiceImpl")
    public RpcTenantDsMetadataProvider rpcTenantDsMetadataProvider() {
        return new RpcTenantDsMetadataProvider();
    }

    @Bean
    @ConditionalOnMissingBean(OperLogRecorder.class)
    @ConditionalOnMissingClass("vip.mseapp.service.system.log.ServiceOperLogRecorder")
    public RpcOperLogRecorder rpcOperLogRecorder() {
        return new RpcOperLogRecorder();
    }
}
