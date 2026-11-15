package vip.mseapp.core.constant;

/**
 * 全局常量。
 */
public final class MseConstants {

    private MseConstants() {
    }

    /** 请求头：租户编号（网关/前端透传，微服务间由 RpcContext 透传） */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** 请求头：链路追踪 ID */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** RPC attachment key：租户编号 */
    public static final String RPC_TENANT_KEY = "mse-tenant-id";

    /** RPC attachment key：当前用户 ID */
    public static final String RPC_USER_KEY = "mse-user-id";

    /** RPC attachment key：当前用户名 */
    public static final String RPC_USERNAME_KEY = "mse-username";

    /** RPC attachment key：平台会话标记（"1"=平台账号体系；provider 恢复 UserContext.isPlatform 用） */
    public static final String RPC_PLATFORM_KEY = "mse-platform";

    /** RPC attachment key：链路追踪 ID */
    public static final String RPC_TRACE_KEY = "mse-trace-id";

    /** MQ 消息 userProperty key：租户编号 */
    public static final String MQ_TENANT_KEY = "mse-tenant-id";

    /** 平台级租户编号：0 表示不隶属任何租户（平台管理员、全局表） */
    public static final long PLATFORM_TENANT_ID = 0L;

    /** 租户判别列名 */
    public static final String TENANT_ID_COLUMN = "tenant_id";

    /** 逻辑删除：未删除 */
    public static final Integer NOT_DELETED = 0;

    /** 通用状态：启用 */
    public static final Integer STATUS_ENABLE = 0;

    /** 通用状态：停用 */
    public static final Integer STATUS_DISABLE = 1;

    /** 登录账号类型：租户端 */
    public static final String LOGIN_TYPE_TENANT = "tenant";

    /** 登录账号类型：平台运营端 */
    public static final String LOGIN_TYPE_PLATFORM = "platform";

    /** Sa-Token 请求头/参数名 */
    public static final String TOKEN_NAME = "Authorization";
}
