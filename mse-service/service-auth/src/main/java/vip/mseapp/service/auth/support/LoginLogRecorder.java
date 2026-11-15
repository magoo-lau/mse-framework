package vip.mseapp.service.auth.support;

import vip.mseapp.api.system.RemoteLogService;
import vip.mseapp.api.system.dto.LoginLogDTO;
import vip.mseapp.core.annotation.RpcReference;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录日志投递（认证模块 → service-system）。
 *
 * <p>日志链路任何异常都不得影响登录本身，全部吞掉仅打印 warn；
 * IP/UA 解析规则在 {@link ClientUa}（与登录会话终端信息共用）。
 */
public class LoginLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(LoginLogRecorder.class);

    @RpcReference
    private RemoteLogService remoteLogService;

    /**
     * 记录一条登录日志。
     *
     * @param tenantId 租户编号（租户未定位到的失败记录传 0）
     * @param username 登录账号
     * @param success  是否成功
     * @param msg      提示消息
     */
    public void record(HttpServletRequest request, Long tenantId, String username, boolean success, String msg) {
        try {
            String ua = request.getHeader("User-Agent");
            LoginLogDTO dto = new LoginLogDTO();
            dto.setTenantId(tenantId == null ? 0L : tenantId);
            dto.setUsername(username);
            dto.setIp(ClientUa.resolveIp(request));
            dto.setBrowser(ClientUa.resolveBrowser(ua));
            dto.setOs(ClientUa.resolveOs(ua));
            dto.setStatus(success ? 0 : 1);
            dto.setMsg(msg);
            remoteLogService.recordLoginLog(dto);
        } catch (Exception ex) {
            log.warn("登录日志记录失败：{}", username, ex);
        }
    }
}
