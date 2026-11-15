package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 组织角色视图（RemoteOrgService 契约）。
 */
public class OrgRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    public OrgRoleDTO() {
    }

    public OrgRoleDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
