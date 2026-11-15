package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.LoginLogDTO;
import vip.mseapp.api.system.dto.OperLogDTO;

/**
 * 日志服务契约（service-system 实现）：登录日志由认证模块投递，操作日志由
 * 未装配 service-system 的业务微服务经 starter-bridge 投递。
 */
public interface RemoteLogService {

    /**
     * 记录一条登录日志（成功与失败均记录；实现内部按 tenantId 显式建上下文，登录链路无租户上下文也安全）。
     *
     * @param log 登录日志事件
     */
    void recordLoginLog(LoginLogDTO log);

    /**
     * 记录一条操作日志（common-web @OperLog 切面的 RPC 落库通道）。
     *
     * @param log 操作日志事件
     */
    void recordOperLog(OperLogDTO log);
}
