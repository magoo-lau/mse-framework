package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.util.Set;

/**
 * 数据范围（RemoteDataScopeService 契约，common-mybatis DataScopeParam 的跨服务形态）。
 *
 * <p>合并语义与 {@code DataScopeResolverImpl} 一致：任一角色为「全部」→ 契约返回 null；
 * 部门类范围（自定义/本部门/本部门及以下）合并为部门并集，type=CUSTOM；
 * 仅剩「仅本人」→ type=SELF。
 */
public class DataScopeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围类型：自定义部门集合（并集，见 deptIds） */
    public static final String TYPE_CUSTOM = "CUSTOM";

    /** 范围类型：仅本人（按 userId 过滤归属） */
    public static final String TYPE_SELF = "SELF";

    /** CUSTOM / SELF（取值见本类常量，与 common-mybatis DataScopeParam.Type 枚举名一致） */
    private String type;

    private Long userId;

    /** 用户所属部门编号（解析「本部门」语义的锚点） */
    private Long deptId;

    /** 可见部门集合（CUSTOM 时非空；已含「本部门及以下」展开结果） */
    private Set<Long> deptIds;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Set<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Set<Long> deptIds) {
        this.deptIds = deptIds;
    }
}
