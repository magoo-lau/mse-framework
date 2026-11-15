package vip.mseapp.mybatis.datasource;

import lombok.Data;

/**
 * 租户数据源定义（sys_tenant_datasource 行的运行时载体）。
 *
 * <p>库模式：shared 共享库（多个租户绑定同一库，tenant_id 行级隔离）/
 * dedicated 独立库（单租户独占）。
 */
@Data
public class TenantDatasourceDefinition {

    public static final String MODE_SHARED = "shared";
    public static final String MODE_DEDICATED = "dedicated";

    private Long id;

    /** 数据源 key（租户隔离配置绑定引用） */
    private String dsKey;

    /** 库模式：shared / dedicated */
    private String dsMode;

    /** 所属服务（spring.application.name，如 mse-oa；'*'=通用） */
    private String serviceName;

    /** 数据库类型：mysql / dm / kingbase */
    private String dbType;

    private String host;

    private Integer port;

    /** 库名（MySQL/金仓为 database；达梦为 schema） */
    private String dbName;

    /** 额外 JDBC 参数（追加到连接串） */
    private String params;

    private String username;

    /** 密文（AES，密钥 mse.tenant.dynamic-ds.encrypt-key） */
    private String password;

    /** 0 启用 1 停用 */
    private Integer status;


    private String remark;

    /** 连接目标摘要（日志/界面提示用，不含凭据） */
    public String target() {
        return dbType + "://" + host + ":" + port + "/" + dbName;
    }
}
