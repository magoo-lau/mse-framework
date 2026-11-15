package vip.mseapp.security.stp;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import vip.mseapp.core.constant.MseConstants;

/**
 * 平台运营端账号体系（loginType = platform）：平台管理员跨租户管理，loginId 即用户编号。
 */
public final class StpPlatformUtil {

    public static final StpLogic stpLogic = new StpLogic(MseConstants.LOGIN_TYPE_PLATFORM);

    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_NICKNAME = "nickname";

    private StpPlatformUtil() {
    }

    public static void login(Long userId, String username, String nickname) {
        login(userId, username, nickname, new SaLoginParameter());
    }

    /** 指定登录参数签发（设备类型/终端扩展信息由调用方组装，见 LoginSessionManager） */
    public static void login(Long userId, String username, String nickname, SaLoginParameter parameter) {
        stpLogic.login(userId, parameter);
        SaSession session = stpLogic.getSession();
        session.set(SESSION_USERNAME, username);
        session.set(SESSION_NICKNAME, nickname);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static Long getUserId() {
        return stpLogic.getLoginIdAsLong();
    }

    public static String getUsername() {
        SaSession session = stpLogic.getSession();
        Object username = session.get(SESSION_USERNAME);
        return username == null ? null : username.toString();
    }

    public static SaSession getSession() {
        return stpLogic.getSession();
    }
}
