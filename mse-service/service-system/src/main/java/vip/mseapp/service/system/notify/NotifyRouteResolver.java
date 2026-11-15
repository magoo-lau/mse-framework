package vip.mseapp.service.system.notify;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import vip.mseapp.service.system.entity.SysNotifyRoute;
import vip.mseapp.service.system.mapper.SysNotifyRouteMapper;

/**
 * 通知路由解析（只读查表，无其他协作依赖——供派发器与路由配置服务共用，避免二者互相依赖成环）。
 *
 * <p>解析链 {@code (biz,type)} 精确 → {@code (biz,*)} → {@code (*,*)} → 兜底 [inbox]
 * （未配置默认仅站内信，存量行为兼容）；无缓存、保存即生效。查表异常（如平台库未建
 * 路由表的世界）按无配置处理——投递尽力而为，不因路由表缺失阻断通知。
 */
@Service
public class NotifyRouteResolver {

    private static final Logger log = LoggerFactory.getLogger(NotifyRouteResolver.class);

    /** 框架内置站内信渠道码（默认兜底渠道） */
    public static final String CHANNEL_INBOX = "inbox";

    private final SysNotifyRouteMapper routeMapper;

    public NotifyRouteResolver(SysNotifyRouteMapper routeMapper) {
        this.routeMapper = routeMapper;
    }

    /** 渠道解析（详见类注释解析链；业务方发送不携带渠道时由派发器调用） */
    public List<String> resolveChannels(String bizCode, String msgType) {
        String biz = normalize(bizCode);
        String type = normalize(msgType);
        SysNotifyRoute hit = find(biz, type, true);
        if (hit == null && !SysNotifyRoute.WILDCARD.equals(biz)) {
            hit = find(biz, SysNotifyRoute.WILDCARD, true);
        }
        if (hit == null && !(SysNotifyRoute.WILDCARD.equals(biz)
                && SysNotifyRoute.WILDCARD.equals(type))) {
            hit = find(SysNotifyRoute.WILDCARD, SysNotifyRoute.WILDCARD, true);
        }
        if (hit == null) {
            return List.of(CHANNEL_INBOX);
        }
        Set<String> codes = split(hit.getChannels());
        return codes.isEmpty() ? List.of(CHANNEL_INBOX) : List.copyOf(codes);
    }

    private SysNotifyRoute find(String bizCode, String msgType, boolean enabledOnly) {
        try {
            return routeMapper.selectOne(new LambdaQueryWrapper<SysNotifyRoute>()
                    .eq(SysNotifyRoute::getBizCode, bizCode)
                    .eq(SysNotifyRoute::getMsgType, msgType)
                    .eq(enabledOnly, SysNotifyRoute::getStatus, SysNotifyRoute.STATUS_ENABLED)
                    .last("LIMIT 1"));
        } catch (Exception e) {
            log.debug("[notify] 租户路由表不可用（按无配置处理）: biz={}, type={} - {}", bizCode, msgType, e.getMessage());
            return null;
        }
    }

    /** 空值归一为通配 {@code *}；业务值 trim */
    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return SysNotifyRoute.WILDCARD;
        }
        return value.trim();
    }

    static Set<String> split(String csv) {
        Set<String> codes = new LinkedHashSet<>();
        if (csv != null) {
            for (String code : csv.split(",")) {
                if (!code.isBlank()) {
                    codes.add(code.trim());
                }
            }
        }
        return codes;
    }
}
