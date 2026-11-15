package vip.mseapp.service.auth.support;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.parameter.SaLogoutParameter;
import cn.dev33.satoken.stp.parameter.enums.SaLogoutMode;
import jakarta.servlet.http.HttpServletRequest;
import vip.mseapp.security.session.MseSessionSupport;

/**
 * 登录会话签发支撑：按登录策略顶号，并组装登录参数。
 *
 * <p>顶号用 REPLACED 模式（被顶端收到「账号已在其他设备登录」），
 * 与管理员强制下线（kickout，收到「已被强制下线」）语义区分。
 * 登录参数带设备类型与终端扩展信息（IP/浏览器/OS），供在线会话管理展示。
 */
public class LoginSessionManager {

    /** 登录时未传设备类型的默认值 */
    public static final String DEFAULT_DEVICE = "pc";

    /**
     * 按策略处理旧会话并组装登录参数（随后交给 Stp*Util.login 重载签发）。
     *
     * @param stpLogic 目标账号体系
     * @param loginId  登录标识（tenant: tenantId-userId；platform: userId）
     * @param policy   已解析的登录策略
     * @param device   登录设备类型（可空，默认 pc）
     * @param request  登录请求（解析 IP/UA）
     */
    public SaLoginParameter prepare(StpLogic stpLogic, Object loginId,
                                    LoginPolicyService.LoginPolicy policy, String device,
                                    HttpServletRequest request) {
        String deviceType = normalizeDevice(device);
        if (!policy.multiDevice()) {
            // 单点：顶掉该账号全部旧会话
            stpLogic.logout(loginId, new SaLogoutParameter().setMode(SaLogoutMode.REPLACED));
        } else if (policy.sameDeviceExclusive()) {
            // 同端互斥：多端并存，但同设备只保留最新会话
            SaLogoutParameter logoutParameter = new SaLogoutParameter()
                    .setDeviceType(deviceType)
                    .setMode(SaLogoutMode.REPLACED);
            stpLogic.logout(loginId, logoutParameter);
        }
        String ua = request.getHeader("User-Agent");
        return new SaLoginParameter()
                .setDeviceType(deviceType)
                .setTerminalExtra(MseSessionSupport.EXTRA_IP, ClientUa.resolveIp(request))
                .setTerminalExtra(MseSessionSupport.EXTRA_BROWSER, ClientUa.resolveBrowser(ua))
                .setTerminalExtra(MseSessionSupport.EXTRA_OS, ClientUa.resolveOs(ua));
    }

    private String normalizeDevice(String device) {
        return device == null || device.isBlank() ? DEFAULT_DEVICE : device.trim();
    }
}
