package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteDeptService;
import vip.mseapp.api.system.dto.DeptDTO;
import vip.mseapp.api.system.dto.DeptTreeNodeDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.service.system.entity.SysDept;
import vip.mseapp.service.system.service.SysDeptService;

import java.util.List;

/**
 * 部门服务契约实现（租户端/平台端各自维护本域部门树，租户上下文经调用方透传）。
 */
@RpcService(RemoteDeptService.class)
public class RemoteDeptServiceImpl implements RemoteDeptService {

    private final SysDeptService deptService;

    public RemoteDeptServiceImpl(SysDeptService deptService) {
        this.deptService = deptService;
    }

    @Override
    public List<DeptTreeNodeDTO> getDeptTree() {
        return deptService.getDeptTree().stream().map(RemoteDeptServiceImpl::toTreeNode).toList();
    }

    @Override
    public Long createDept(Long parentId, String name, Integer sort) {
        return deptService.createDept(parentId, name, sort);
    }

    @Override
    public void updateDept(Long id, Long parentId, String name, Integer sort, Integer status) {
        deptService.updateDept(id, parentId, name, sort, status);
    }

    @Override
    public void deleteDept(Long id) {
        deptService.deleteDept(id);
    }

    private static DeptTreeNodeDTO toTreeNode(SysDeptService.DeptTreeNode node) {
        SysDept dept = node.dept();
        DeptDTO dto = new DeptDTO();
        dto.setId(dept.getId());
        dto.setParentId(dept.getParentId());
        dto.setAncestors(dept.getAncestors());
        dto.setName(dept.getName());
        dto.setSort(dept.getSort());
        dto.setStatus(dept.getStatus());
        return new DeptTreeNodeDTO(dto, node.children().stream().map(RemoteDeptServiceImpl::toTreeNode).toList());
    }
}
