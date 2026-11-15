package vip.mseapp.mybatis.schema;

/**
 * 脚本目标库（头指令 {@code -- mse-target:}）：脚本按目标路由到不同库执行，各库独立账本
 * （{@code mse_<biz>_schema_history} 同名表分库各持一份、行集互不重叠）。
 */
public enum MigrationTarget {

    /** 业务主库（缺省）：本服务业务表所在库，并随租户库克隆/懒初始化进各租户库 */
    MASTER,

    /**
     * 基座共享库：mse-system 主库（sys_menu/sys_permission/sys_micro_app 等共用表所在）。
     * 业务积木的注册脚本经 starter-bridge 推送 mse-system 应用；单体/基座应用（classpath 含
     * service-system）启动时本地直接应用。
     */
    BASE,

    /** 平台控制面库：mse-platform 主库（sys_tenant 系控制面表所在），仅平台侧脚本使用 */
    PLATFORM,

    /**
     * 租户库结构：基座身份表等 BaseEntity 表的 DDL（sys_user/sys_role/... + undo_log），
     * 仅在租户库/共享库直建时执行（{@code TenantDsLazyInitializer} 脚本直建 + 启动迁移对
     * primary 兜底应用）；平台库身份表由 BASE 通道建，两通道脚本各自幂等互不冲突。
     */
    TENANT;

    /** 头指令值解析（null/blank = 缺省 master；未知值抛异常，避免脚本发错库） */
    public static MigrationTarget of(String value) {
        if (value == null || value.isBlank()) {
            return MASTER;
        }
        return switch (value.trim().toLowerCase()) {
            case "master" -> MASTER;
            case "base" -> BASE;
            case "platform" -> PLATFORM;
            case "tenant" -> TENANT;
            default -> throw new IllegalStateException("未知 mse-target 指令值: " + value);
        };
    }
}
