package vip.mseapp.service.system.service;

import vip.mseapp.core.result.PageResult;

import java.time.LocalDateTime;

/**
 * 在线会话管理（Sa-Token Redis 会话直查，无业务表）：列表来自终端清单（SaTerminalInfo），
 * 对外只暴露不可逆会话标识（SHA-256 摘要前 16 位），原始 token 永不出服务端。
 */
public interface SysOnlineService {

    /**
     * 在线会话分页（按登录时间倒序，内存分页）。
     *
     * <p>每个世界只能看自己世界的会话：0（平台）只见平台账号体系会话，
     * N 只见租户 N 的租户账号体系会话。
     *
     * @param tenantScope 操作者世界编号（0=平台这一特殊租户）
     * @param keyword     用户名/昵称关键字（模糊包含，可空）
     */
    PageResult<OnlineSession> pageSessions(long pageNum, long pageSize, long tenantScope, String keyword);

    /**
     * 下线单条会话（按会话标识定位真实 token）。
     *
     * @param tenantScope 操作者世界编号：0 只允许下线平台账号体系会话；否则只允许下线该租户的租户端会话
     */
    void kickSession(long tenantScope, KickCommand command);

    /** 下线某账号的全部会话（世界范围校验同 {@link #kickSession}） */
    void kickUser(long tenantScope, KickCommand command);

    /** 下线指令：定位会话归属（loginType + 租户/用户编号）与会话标识 */
    record KickCommand(String loginType, Long tenantId, Long userId, String sessionId) {
    }

    /** 在线会话条目（sessionId 为 SHA-256 摘要前 16 位，非原始 token；tenantName 为展示用租户名称，平台端会话为"平台"） */
    record OnlineSession(String loginType, String sessionId, Long tenantId, String tenantName, Long userId,
                         String username, String nickname, String deviceType,
                         String ip, String browser, String os, LocalDateTime loginTime) {
    }
}
