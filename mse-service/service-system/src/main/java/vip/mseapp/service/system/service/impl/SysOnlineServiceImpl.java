package vip.mseapp.service.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.util.StringUtils;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.security.session.MseSessionSupport;
import vip.mseapp.security.stp.StpPlatformUtil;
import vip.mseapp.security.stp.StpTenantUtil;
import vip.mseapp.service.system.service.SysOnlineService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线会话管理实现：经 token 键空间扫描发现全部在线会话（searchTokenValue），
 * 再按 loginId 回查账号会话（用户名/昵称）与终端清单（设备/IP/浏览器/登录时间）。
 *
 * <p>扫描带总量上限（防御性：会话规模异常膨胀时截断，避免大 Redis 全量遍历拖垮管理页）。
 */
public class SysOnlineServiceImpl implements SysOnlineService {

    /** 单次扫描分页大小 */
    private static final int SCAN_PAGE_SIZE = 500;
    /** 单端扫描总量上限 */
    private static final int SCAN_MAX = 10000;

    /** 租户名称归 service-tenant 持有，经 RPC 契约查询（单体下本地直调） */
    @RpcReference
    private RemoteTenantService remoteTenantService;

    @Override
    public PageResult<OnlineSession> pageSessions(long pageNum, long pageSize, long tenantScope, String keyword) {
        List<OnlineSession> all = new ArrayList<>();
        if (MseConstants.PLATFORM_TENANT_ID == tenantScope) {
            // 平台世界（0=特殊租户）：只看平台账号体系自身的会话，租户会话由各租户端自管
            collectFrom(StpPlatformUtil.stpLogic, MseConstants.LOGIN_TYPE_PLATFORM, null, all);
        } else {
            collectFrom(StpTenantUtil.stpLogic, MseConstants.LOGIN_TYPE_TENANT, tenantScope, all);
        }
        if (StringUtils.hasText(keyword)) {
            all.removeIf(session -> !matchKeyword(session, keyword));
        }
        all.sort(Comparator.comparing(OnlineSession::loginTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        long total = all.size();
        int from = (int) Math.min((pageNum - 1) * pageSize, total);
        int to = (int) Math.min(from + pageSize, total);
        return PageResult.of(total, pageNum, pageSize, withTenantNames(all.subList(from, to)));
    }

    @Override
    public void kickSession(long tenantScope, KickCommand command) {
        checkScope(tenantScope, command);
        StpLogic stpLogic = stpLogicOf(command.loginType());
        if (!MseSessionSupport.kickBySessionId(stpLogic, loginIdOf(command), command.sessionId())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "会话不存在或已下线");
        }
    }

    @Override
    public void kickUser(long tenantScope, KickCommand command) {
        checkScope(tenantScope, command);
        stpLogicOf(command.loginType()).kickout(loginIdOf(command));
    }

    // -------------------- 会话收集与构建 --------------------

    /** 扫描某账号体系的 token 键空间，构建会话条目（tenantScope 只对租户端生效） */
    private void collectFrom(StpLogic stpLogic, String loginType, Long tenantScope, List<OnlineSession> out) {
        for (int start = 0; start < SCAN_MAX; start += SCAN_PAGE_SIZE) {
            List<String> keys = stpLogic.searchTokenValue("", start, SCAN_PAGE_SIZE, false);
            for (String key : keys) {
                String tokenValue = key.substring(key.lastIndexOf(':') + 1);
                OnlineSession session = buildSession(stpLogic, loginType, tokenValue, tenantScope);
                if (session != null) {
                    out.add(session);
                }
            }
            if (keys.size() < SCAN_PAGE_SIZE) {
                break;
            }
        }
    }

    /** token → 会话条目；loginId 失效（过期/被踢残留标记）或不在租户范围时返回 null */
    private OnlineSession buildSession(StpLogic stpLogic, String loginType, String tokenValue, Long tenantScope) {
        Object loginId = stpLogic.getLoginIdByToken(tokenValue);
        if (loginId == null || loginId.toString().startsWith("-")) {
            // 键空间残留：正常值为 loginId，被顶/被踢后为负数标记
            return null;
        }
        Long tenantId;
        Long userId;
        if (MseConstants.LOGIN_TYPE_TENANT.equals(loginType)) {
            String[] parts = loginId.toString().split("-");
            tenantId = Long.parseLong(parts[0]);
            userId = Long.parseLong(parts[1]);
            if (tenantScope != null && !tenantScope.equals(tenantId)) {
                return null;
            }
        } else {
            userId = Long.parseLong(loginId.toString());
            tenantId = MseConstants.PLATFORM_TENANT_ID;
        }
        SaSession accountSession = stpLogic.getSessionByLoginId(loginId, false);
        SaTerminalInfo terminal = findTerminal(accountSession, tokenValue);
        return new OnlineSession(
                loginType,
                MseSessionSupport.sessionId(tokenValue),
                tenantId,
                null,
                userId,
                stringOf(accountSession, "username"),
                stringOf(accountSession, "nickname"),
                terminal == null ? "pc" : MseSessionSupport.deviceType(terminal),
                terminal == null ? null : MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_IP),
                terminal == null ? null : MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_BROWSER),
                terminal == null ? null : MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_OS),
                terminal == null ? null : toLocalDateTime(terminal.getCreateTime()));
    }

    /** 从账号会话的终端清单中定位 token 对应终端 */
    private SaTerminalInfo findTerminal(SaSession accountSession, String tokenValue) {
        if (accountSession == null) {
            return null;
        }
        for (SaTerminalInfo terminal : accountSession.getTerminalList()) {
            if (tokenValue.equals(terminal.getTokenValue())) {
                return terminal;
            }
        }
        return null;
    }

    /** 当前页行补租户名称（只查本页涉及的租户；平台端会话固定"平台"，解析失败退回编号展示不阻塞列表） */
    private List<OnlineSession> withTenantNames(List<OnlineSession> page) {
        Map<Long, String> names = new HashMap<>();
        List<OnlineSession> result = new ArrayList<>(page.size());
        for (OnlineSession session : page) {
            String tenantName = MseConstants.LOGIN_TYPE_PLATFORM.equals(session.loginType())
                    ? "平台"
                    : names.computeIfAbsent(session.tenantId(), this::tenantNameOf);
            result.add(new OnlineSession(session.loginType(), session.sessionId(), session.tenantId(), tenantName,
                    session.userId(), session.username(), session.nickname(), session.deviceType(),
                    session.ip(), session.browser(), session.os(), session.loginTime()));
        }
        return result;
    }

    private String tenantNameOf(Long tenantId) {
        try {
            TenantDTO tenant = remoteTenantService.getTenantById(tenantId);
            if (tenant != null && StringUtils.hasText(tenant.getName())) {
                return tenant.getName();
            }
        } catch (Exception ignored) {
            // 租户服务不可用/租户已删除时退回编号展示
        }
        return "租户 " + tenantId;
    }

    private String stringOf(SaSession session, String key) {
        if (session == null) {
            return null;
        }
        Object value = session.get(key);
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }

    private boolean matchKeyword(OnlineSession session, String keyword) {
        return contains(session.username(), keyword) || contains(session.nickname(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    // -------------------- 下线与范围校验 --------------------

    /** 世界范围校验：0（平台）只允许操作平台账号体系会话；租户世界只允许操作本租户账号的租户端会话（防跨世界踢人） */
    private void checkScope(long tenantScope, KickCommand command) {
        if (MseConstants.PLATFORM_TENANT_ID == tenantScope) {
            if (!MseConstants.LOGIN_TYPE_PLATFORM.equals(command.loginType())) {
                throw new ServiceException(GlobalErrorCode.FORBIDDEN);
            }
            return;
        }
        if (!MseConstants.LOGIN_TYPE_TENANT.equals(command.loginType())
                || command.tenantId() == null || !command.tenantId().equals(tenantScope)) {
            throw new ServiceException(GlobalErrorCode.FORBIDDEN);
        }
    }

    private StpLogic stpLogicOf(String loginType) {
        return MseConstants.LOGIN_TYPE_PLATFORM.equals(loginType)
                ? StpPlatformUtil.stpLogic
                : StpTenantUtil.stpLogic;
    }

    private Object loginIdOf(KickCommand command) {
        if (MseConstants.LOGIN_TYPE_PLATFORM.equals(command.loginType())) {
            return command.userId();
        }
        return command.tenantId() + "-" + command.userId();
    }
}
