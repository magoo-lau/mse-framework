package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 登录日志（认证模块 → service-system 投递；字段与 SysLogininfor 实体对应）。
 */
public class LoginLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户编号（租户不存在等失败场景传 0） */
    private Long tenantId;
    private String username;
    private String ip;
    private String browser;
    private String os;
    /** 0 成功 1 失败 */
    private Integer status;
    /** 提示消息 */
    private String msg;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
