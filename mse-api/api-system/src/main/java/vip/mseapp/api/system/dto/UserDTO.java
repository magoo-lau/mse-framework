package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 用户信息（内部 RPC 契约，字段与 SysUser 实体对应）。
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String username;
    /** BCrypt 散列，仅限内部认证链路使用（@JsonIgnore：任何 HTTP JSON 响应不透出） */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    /** 部门编号（数据权限锚点） */
    private Long deptId;
    private Integer status;
    /** 是否该租户的管理员（拥有租户内全部权限） */
    private Boolean superAdmin;
    /** 是否已绑定 TOTP 双因子（登录链路判定是否进入动态码步骤；密钥本体绝不出 service-system） */
    private Boolean totpBound;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(Boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public Boolean getTotpBound() {
        return totpBound;
    }

    public void setTotpBound(Boolean totpBound) {
        this.totpBound = totpBound;
    }
}
