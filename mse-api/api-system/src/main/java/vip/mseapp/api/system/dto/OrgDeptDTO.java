package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 组织部门视图（RemoteOrgService 契约，ancestors 供主管链上溯）。
 */
public class OrgDeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String ancestors;
    private Long leaderUserId;

    public OrgDeptDTO() {
    }

    public OrgDeptDTO(Long id, Long parentId, String name, String ancestors) {
        this(id, parentId, name, ancestors, null);
    }

    public OrgDeptDTO(Long id, Long parentId, String name, String ancestors, Long leaderUserId) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.ancestors = ancestors;
        this.leaderUserId = leaderUserId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    public Long getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(Long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }
}
