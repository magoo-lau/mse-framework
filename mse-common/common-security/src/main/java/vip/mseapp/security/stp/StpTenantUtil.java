package vip.mseapp.security.stp;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import vip.mseapp.core.constant.MseConstants;

/**
 * 租户端账号体系（loginType = tenant）。
 *
 * <p>loginId 约定为 {@code tenantId-userId}（Sa-Token 禁止冒号），天然携带租户归属，
 * TenantTokenSyncInterceptor 借此将 token 解析出的租户覆盖请求头，防止伪造越权。
 */
public final class StpTenantUtil {

    public static final StpLogic stpLogic = new StpLogic(MseConstants.LOGIN_TYPE_TENANT);

    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_NICKNAME = "nickname";

    private StpTenantUtil() {
    }

    /** loginId 格式：{tenantId}-{userId} */
    public static String buildLoginId(Long tenantId, Long userId) {
        return tenantId + "-" + userId;
    }

    public static void login(Long tenantId, Long userId, String username, String nickname) {
        login(tenantId, userId, username, nickname, new SaLoginParameter());
    }

    /** 指定登录参数签发（设备类型/终端扩展信息由调用方组装，见 LoginSessionManager） */
    public static void login(Long tenantId, Long userId, String username, String nickname, SaLoginParameter parameter) {
        stpLogic.login(buildLoginId(tenantId, userId), parameter);
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

    public static String getLoginIdAsString() {
        return stpLogic.getLoginIdAsString();
    }

    /** 从 loginId（tenantId-userId）解析租户编号 */
    public static Long getTenantIdFromLoginId() {
        return Long.parseLong(stpLogic.getLoginIdAsString().split("-")[0]);
    }

    /** 从 loginId（tenantId-userId）解析用户编号 */
    public static Long getUserIdFromLoginId() {
        return Long.parseLong(stpLogic.getLoginIdAsString().split("-")[1]);
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
