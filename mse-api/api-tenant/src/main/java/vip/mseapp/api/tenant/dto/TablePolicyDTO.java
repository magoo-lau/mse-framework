package vip.mseapp.api.tenant.dto;

import java.io.Serializable;

/**
 * 表级隔离策略（内部 RPC 契约）。
 */
public class TablePolicyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 逻辑表名（小写） */
    private String tableName;

    /** 表级模式：SPLIT / FIELD / GLOBAL */
    private String mode;

    /** 分表后缀（SPLIT 模式用；空 = 服务级 table_suffix） */
    private String tableSuffix;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }

    public void setTableSuffix(String tableSuffix) {
        this.tableSuffix = tableSuffix;
    }
}
