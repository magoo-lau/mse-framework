package vip.mseapp.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 当前登录用户上下文（认证成功后由 common-security 写入）。
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new TransmittableThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo user) {
        HOLDER.set(user);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserInfo user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static String getUsername() {
        UserInfo user = HOLDER.get();
        return user == null ? null : user.username();
    }

    public static boolean isPlatform() {
        UserInfo user = HOLDER.get();
        return user != null && user.platform();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户简要信息。
     *
     * @param userId   用户编号
     * @param username 登录名
     * @param platform 是否平台运营端账号
     */
    public record UserInfo(Long userId, String username, boolean platform) {
    }
}
