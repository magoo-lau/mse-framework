package vip.mseapp.core.datasource;

/**
 * 表级隔离策略（sys_tenant_service_table_policy，覆盖服务级默认）。
 *
 * <p>说明：受 MyBatis-Plus TenantLineHandler 接口限制，隔离列名仅支持服务级配置
 * （tenant_column），表级策略只覆盖「模式 + 分表后缀」。
 */
public class TablePolicy {

    public enum Mode {
        /** 按租户分表：逻辑表 + 后缀 */
        SPLIT,
        /** 按租户字段隔离：追加 tenant 条件 */
        FIELD,
        /** 全局共享表：不改写 */
        GLOBAL
    }

    private final Mode mode;

    /** 分表后缀（SPLIT 模式必填），缺省用服务级 table_suffix */
    private final String tableSuffix;

    public TablePolicy(Mode mode, String tableSuffix) {
        this.mode = mode;
        this.tableSuffix = tableSuffix;
    }

    public Mode getMode() {
        return mode;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }
}
