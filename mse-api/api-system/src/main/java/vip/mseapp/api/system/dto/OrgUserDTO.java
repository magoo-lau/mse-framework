package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 组织用户视图（RemoteOrgService 契约，字段对应 SysUser 的组织相关子集）。
 */
public class OrgUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private Long deptId;
    private String mobile;
    private String avatar;
    private Integer status;

    public OrgUserDTO() {
    }

    public OrgUserDTO(Long id, String username, String nickname, Long deptId, String mobile, String avatar) {
        this(id, username, nickname, deptId, mobile, avatar, null);
    }

    public OrgUserDTO(Long id, String username, String nickname, Long deptId, String mobile, String avatar, Integer status) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.deptId = deptId;
        this.mobile = mobile;
        this.avatar = avatar;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /** 0 启用 1 停用 */
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
