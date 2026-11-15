package vip.mseapp.service.system.notify;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.mseapp.api.notify.RemoteNotifyService;
import vip.mseapp.api.notify.dto.NotifyChannelView;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysNotifyRoute;
import vip.mseapp.service.system.mapper.SysNotifyRouteMapper;

/**
 * 通知路由配置（业务动作 → 渠道，租户隔离的框架级配置）：分页、保存与校验、发送测试。
 * 业务方不再自持路由表——发送不携带渠道时由派发器经 {@link NotifyRouteResolver} 按租户路由
 * 解析（v3 上收，见 docs/tech/notify.md）。保存校验：所选渠道在目录已登记且当前租户
 * 已授权；业务来源/消息类型须已登记或为通配 {@code *}（目录 = 本地 Provider Bean +
 * sys_notify_source 登记行，未部署提供方的历史配置仍可保存编辑，仅提示）。
 */
@Service
public class SysNotifyRouteService {

    private final SysNotifyRouteMapper routeMapper;
    private final SysNotifyCatalogService catalogService;
    private final RemoteNotifyService notifyService;

    public SysNotifyRouteService(SysNotifyRouteMapper routeMapper,
                                 SysNotifyCatalogService catalogService,
                                 RemoteNotifyService notifyService) {
        this.routeMapper = routeMapper;
        this.catalogService = catalogService;
        this.notifyService = notifyService;
    }

    public Page<SysNotifyRoute> page(long pageNum, long pageSize, String bizCode, String msgType) {
        return routeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysNotifyRoute>()
                        .eq(bizCode != null && !bizCode.isBlank(), SysNotifyRoute::getBizCode, bizCode)
                        .eq(msgType != null && !msgType.isBlank(), SysNotifyRoute::getMsgType, msgType)
                        .orderByDesc(SysNotifyRoute::getId));
    }

    /** 来源目录（本地 Provider + 登记行合并，配置页「业务来源/消息类型」下拉数据源） */
    public List<SysNotifyCatalogService.SourceEntry> listSources() {
        return catalogService.sources();
    }

    /** 渠道目录（当前租户授权态+可用态；配置页勾选数据源） */
    public List<NotifyChannelView> listChannels() {
        return notifyService.listChannels();
    }

    /**
     * 保存（按 当前租户+biz+type upsert）。校验所选渠道在框架目录已登记且当前租户已授权
     * （未授权渠道在配置页置灰，此处兜底防绕过）。
     *
     * @return 配置 id
     */
    public long save(Long id, String bizCode, String msgType, List<String> channels,
                     Integer status, String remark) {
        String biz = NotifyRouteResolver.normalize(bizCode);
        String type = NotifyRouteResolver.normalize(msgType);
        if (biz.isEmpty() || type.isEmpty()) {
            throw new ServiceException(400, "业务来源与消息类型不能为空");
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String code : channels == null ? List.<String>of() : channels) {
            if (code != null && !code.isBlank()) {
                codes.add(code.trim());
            }
        }
        if (codes.isEmpty()) {
            throw new ServiceException(400, "至少选择一个投递渠道");
        }
        validateChannels(codes);
        SysNotifyRoute route;
        if (id != null) {
            route = routeMapper.selectById(id);
            if (route == null || !TenantContext.getRequiredTenantId().equals(route.getTenantId())) {
                throw new ServiceException(404, "配置不存在");
            }
        } else {
            // upsert 以同组合任意状态行（含停用）为准，避免与停用行撞唯一键
            route = find(biz, type, false);
            if (route == null) {
                route = new SysNotifyRoute();
                route.setBizCode(biz);
                route.setMsgType(type);
            }
        }
        route.setChannels(String.join(",", codes));
        route.setStatus(status == null ? SysNotifyRoute.STATUS_ENABLED : status);
        route.setRemark(remark);
        if (route.getId() == null) {
            routeMapper.insert(route);
        } else {
            routeMapper.updateById(route);
        }
        return route.getId();
    }

    /** 发送测试：向当前登录人按指定渠道同步投递（走中枢 sendSync，即时回显逐渠道结果） */
    public List<vip.mseapp.api.notify.dto.NotifyChannelOutcome> testChannels(List<String> channels) {
        var message = new vip.mseapp.api.notify.dto.NotifyMessage("system", "TEST",
                List.of(vip.mseapp.core.context.UserContext.getUserId()),
                "通知渠道测试", "这是一条测试通知，用于验证所选渠道的连通性。", null, null);
        return notifyService.sendSync(new vip.mseapp.api.notify.dto.NotifySendRequest(channels, message));
    }

    /** 所选渠道须在框架目录已登记且当前租户已授权 */
    private void validateChannels(Set<String> codes) {
        List<NotifyChannelView> channels = notifyService.listChannels();
        for (String code : codes) {
            NotifyChannelView view = channels.stream()
                    .filter(ch -> code.equals(ch.getCode())).findFirst().orElse(null);
            if (view == null) {
                throw new ServiceException(400, "渠道未登记（提供方未部署或已下线）：" + code);
            }
            if (!view.isAuthorized()) {
                throw new ServiceException(403, "渠道未授权（请联系平台开通）：" + view.getName());
            }
        }
    }

    private SysNotifyRoute find(String bizCode, String msgType, boolean enabledOnly) {
        return routeMapper.selectOne(new LambdaQueryWrapper<SysNotifyRoute>()
                .eq(SysNotifyRoute::getBizCode, bizCode)
                .eq(SysNotifyRoute::getMsgType, msgType)
                .eq(enabledOnly, SysNotifyRoute::getStatus, SysNotifyRoute.STATUS_ENABLED)
                .last("LIMIT 1"));
    }
}
