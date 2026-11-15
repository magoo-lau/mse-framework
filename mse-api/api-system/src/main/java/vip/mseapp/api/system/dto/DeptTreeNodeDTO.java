package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 部门树节点（RemoteDeptService 契约）。
 */
public class DeptTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private DeptDTO dept;
    private List<DeptTreeNodeDTO> children;

    public DeptTreeNodeDTO() {
    }

    public DeptTreeNodeDTO(DeptDTO dept, List<DeptTreeNodeDTO> children) {
        this.dept = dept;
        this.children = children;
    }

    public DeptDTO getDept() {
        return dept;
    }

    public void setDept(DeptDTO dept) {
        this.dept = dept;
    }

    public List<DeptTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<DeptTreeNodeDTO> children) {
        this.children = children;
    }
}
