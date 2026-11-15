package vip.mseapp.api.tenant.dto;

import java.io.Serializable;

/**
 * 租户数据源定义（内部 RPC 契约，listTenantDatasources / getTenantDatasource 返回）。
 *
 * <p>password 为 AES 密文（密钥 mse.tenant.dynamic-ds.encrypt-key，各服务共享配置），
 * 契约传输不落明文。
 */
public class TenantDatasourceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String dsKey;

    /** 库模式：shared / dedicated */
    private String dsMode;

    /** 所属服务（spring.application.name；'*'=通用） */
    private String serviceName;

    /** 数据库类型：mysql / dm / kingbase */
    private String dbType;

    private String host;

    private Integer port;

    private String dbName;

    private String params;

    private String username;

    /** 密文 */
    private String password;

    /** 0 启用 1 停用 */
    private Integer status;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDsKey() {
        return dsKey;
    }

    public void setDsKey(String dsKey) {
        this.dsKey = dsKey;
    }

    public String getDsMode() {
        return dsMode;
    }

    public void setDsMode(String dsMode) {
        this.dsMode = dsMode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }



    public void setStatus(Integer status) {
        this.status = status;
    }
}
