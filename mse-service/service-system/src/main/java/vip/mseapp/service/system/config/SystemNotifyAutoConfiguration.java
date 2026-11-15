package vip.mseapp.service.system.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import vip.mseapp.api.notify.NotifySourceProvider;
import vip.mseapp.api.notify.RemoteNotifyChannelRegistry;
import vip.mseapp.api.notify.RemoteNotifyService;
import vip.mseapp.redis.config.MseRedisAutoConfiguration.LockOps;
import vip.mseapp.service.system.notify.InboxNotifyChannel;
import vip.mseapp.service.system.notify.NotifyChannelAuthService;
import vip.mseapp.service.system.notify.NotifyChannelRouter;
import vip.mseapp.service.system.notify.NotifyDispatchService;
import vip.mseapp.service.system.notify.NotifyRouteResolver;
import vip.mseapp.service.system.notify.RemoteNotifyChannelRegistryImpl;
import vip.mseapp.service.system.notify.RemoteNotifyServiceImpl;
import vip.mseapp.service.system.notify.SysNotifyCatalogService;
import vip.mseapp.service.system.notify.SysNotifyRouteService;
import vip.mseapp.service.system.notify.SysNotifyWebhookService;
import vip.mseapp.service.system.notify.WebhookNotifyChannel;
import vip.mseapp.service.system.service.SysMessageService;
import vip.mseapp.service.system.service.TenantScopeService;
import vip.mseapp.service.system.mapper.SysNotifyChannelMapper;
import vip.mseapp.service.system.mapper.SysNotifyLogMapper;
import vip.mseapp.service.system.mapper.SysNotifyRouteMapper;
import vip.mseapp.service.system.mapper.SysNotifySourceMapper;
import vip.mseapp.service.system.mapper.SysNotifyWebhookMapper;

/**
 * 框架通知中枢自动装配（消息中心多渠道化）：渠道/来源目录（登记接收 + 本地 Bean 收集）、
 * 租户授权校验（权益开关）、渠道路由、租户路由配置（业务动作 → 渠道，自业务层上收）、
 * 异步派发与投递日志、内置站内信渠道。
 *
 * <p>分层语义：业务层（OA/MES…）发送不携带渠道，由中枢按租户路由 (bizCode,msgType) 解析后
 * 派发——目录/授权/路由/调度/日志全部收敛在本模块，业务零配置页。渠道提供方（mse-im 等）
 * 无需依赖 service-system：微服务形态经 api-notify 推送器登记渠道与来源，单体聚合形态容器内
 * RemoteNotifyChannel / NotifySourceProvider Bean 直接被目录合并收集。控制器
 * {@code SysNotifyController} 由 SystemControllerAutoConfiguration 的组件扫描注册。
 */
@AutoConfiguration
public class SystemNotifyAutoConfiguration {

    /** 渠道/来源目录（登记接收 + 本地 Bean 合并） */
    @Bean
    @ConditionalOnMissingBean
    public SysNotifyCatalogService sysNotifyCatalogService(SysNotifyChannelMapper channelMapper,
                                                           SysNotifySourceMapper sourceMapper,
                                                           ObjectProvider<LockOps> lockOps,
                                                           ObjectProvider<vip.mseapp.api.notify.RemoteNotifyChannel> localChannels,
                                                           ObjectProvider<NotifySourceProvider> localSourceProviders) {
        return new SysNotifyCatalogService(channelMapper, sourceMapper, localChannels, localSourceProviders, lockOps);
    }

    /** 渠道租户授权校验（权益开关，对整租户生效） */
    @Bean
    @ConditionalOnMissingBean
    public NotifyChannelAuthService notifyChannelAuthService(TenantScopeService tenantScopeService) {
        return new NotifyChannelAuthService(tenantScopeService);
    }

    /** 渠道路由（本地 Bean 优先 → Dubbo 按提供组定向） */
    @Bean
    @ConditionalOnMissingBean
    public NotifyChannelRouter notifyChannelRouter(
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.api.notify.RemoteNotifyChannel> localChannels,
            Environment environment) {
        return new NotifyChannelRouter(localChannels, environment);
    }

    /** 租户路由解析（业务动作 → 渠道查表；独立组件避免派发器与配置服务互相依赖成环） */
    @Bean
    @ConditionalOnMissingBean
    public NotifyRouteResolver notifyRouteResolver(SysNotifyRouteMapper routeMapper) {
        return new NotifyRouteResolver(routeMapper);
    }

    /** 派发器（渠道解析 + 异步调度 + 投递日志） */
    @Bean
    @ConditionalOnMissingBean
    public NotifyDispatchService notifyDispatchService(SysNotifyCatalogService catalogService,
                                                       NotifyChannelRouter router,
                                                       NotifyChannelAuthService authService,
                                                       NotifyRouteResolver routeResolver,
                                                       SysNotifyLogMapper logMapper) {
        return new NotifyDispatchService(catalogService, router, authService, routeResolver, logMapper);
    }

    /** 框架内置站内信渠道（直调基座消息中心） */
    @Bean
    @ConditionalOnMissingBean(InboxNotifyChannel.class)
    public InboxNotifyChannel inboxNotifyChannel(SysMessageService messageService) {
        return new InboxNotifyChannel(messageService);
    }

    /** 框架内置 Webhook 群发渠道（钉钉/企微/飞书群机器人，租户级地址配置） */
    @Bean
    @ConditionalOnMissingBean(WebhookNotifyChannel.class)
    public WebhookNotifyChannel webhookNotifyChannel(SysNotifyWebhookService webhookService) {
        return new WebhookNotifyChannel(webhookService);
    }

    /** Webhook 渠道租户级配置（地址/格式，每世界一行） */
    @Bean
    @ConditionalOnMissingBean
    public SysNotifyWebhookService sysNotifyWebhookService(SysNotifyWebhookMapper webhookMapper) {
        return new SysNotifyWebhookService(webhookMapper);
    }

    /** 通知发送契约实现（业务层投递入口）：可声明同契约 Bean 整体替换 */
    @Bean
    @ConditionalOnMissingBean(RemoteNotifyService.class)
    public RemoteNotifyService remoteNotifyService(NotifyDispatchService dispatchService,
                                                   SysNotifyCatalogService catalogService,
                                                   NotifyChannelAuthService authService,
                                                   NotifyChannelRouter router) {
        return new RemoteNotifyServiceImpl(dispatchService, catalogService, authService, router);
    }

    /** 渠道/来源登记契约实现（提供方推送接收）：可声明同契约 Bean 整体替换 */
    @Bean
    @ConditionalOnMissingBean(RemoteNotifyChannelRegistry.class)
    public RemoteNotifyChannelRegistry remoteNotifyChannelRegistry(SysNotifyCatalogService catalogService) {
        return new RemoteNotifyChannelRegistryImpl(catalogService);
    }

    /** 租户路由配置（系统管理「通知渠道设置」页后端） */
    @Bean
    @ConditionalOnMissingBean
    public SysNotifyRouteService sysNotifyRouteService(SysNotifyRouteMapper routeMapper,
                                                       SysNotifyCatalogService catalogService,
                                                       RemoteNotifyService notifyService) {
        return new SysNotifyRouteService(routeMapper, catalogService, notifyService);
    }
}
