package vip.mseapp.core.result;

/**
 * 全局通用错误码。约定：0 成功；4xx 客户端错误；5xx 服务端错误；业务模块错误码从 1000 开始。
 */
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),

    BAD_REQUEST(400, "请求参数不正确"),
    UNAUTHORIZED(401, "账号未登录或会话已过期"),
    FORBIDDEN(403, "没有该操作权限"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    INTERNAL_ERROR(500, "系统内部异常"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    /** 多租户 */
    TENANT_CONTEXT_MISSING(500, "租户上下文缺失，禁止执行未隔离的数据操作"),
    TENANT_NOT_EXISTS(1000, "租户不存在"),
    TENENT_DISABLED(1001, "租户已停用"),
    TENANT_EXPIRED(1002, "租户已过期"),
    TENANT_FORBIDDEN(1003, "无权访问该租户的数据"),

    /** 认证 */
    AUTH_LOGIN_FAILED(1100, "登录失败：用户名或密码错误"),
    AUTH_USER_DISABLED(1101, "用户已停用"),
    AUTH_DOMAIN_TENANT_MISMATCH(1102, "当前域名已绑定租户，仅允许登录绑定的租户"),

    /** 系统管理 */
    SYSTEM_USER_USERNAME_DUPLICATE(1200, "用户名已存在"),
    SYSTEM_ROLE_CODE_DUPLICATE(1201, "角色编码已存在"),
    SYSTEM_TENANT_CODE_DUPLICATE(1202, "租户编码已存在"),
    SYSTEM_PERMISSION_CODE_DUPLICATE(1204, "权限标识已存在"),
    SYSTEM_GRANT_OUT_OF_SCOPE(1205, "授权内容超出可授权范围（套餐/可见域外）"),
    SYSTEM_TENANT_DOMAIN_DUPLICATE(1206, "绑定域名已存在"),
    SYSTEM_POST_CODE_DUPLICATE(1207, "岗位编码已存在"),
    SYSTEM_DICT_TYPE_DUPLICATE(1208, "字典类型已存在"),
    SYSTEM_DICT_TYPE_HAS_DATA(1209, "字典类型下存在数据，无法删除"),
    SYSTEM_CONFIG_KEY_DUPLICATE(1210, "参数键名已存在"),
    SYSTEM_CONFIG_BUILT_IN(1211, "系统内置参数，不允许删除");

    private final int code;
    private final String message;

    GlobalErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
