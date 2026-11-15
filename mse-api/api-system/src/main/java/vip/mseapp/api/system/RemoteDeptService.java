package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.DeptTreeNodeDTO;

import java.util.List;

/**
 * 部门服务契约（service-system 实现）：部门树查询与基础维护。
 *
 * <p>在调用方租户上下文内执行（租户端/平台端各自维护本域部门树）。
 */
public interface RemoteDeptService {

    /** 当前域部门树 */
    List<DeptTreeNodeDTO> getDeptTree();

    /** 创建部门 */
    Long createDept(Long parentId, String name, Integer sort);

    /** 更新部门（parentId 变更时级联刷新子树 ancestors） */
    void updateDept(Long id, Long parentId, String name, Integer sort, Integer status);

    /** 删除部门（有子部门或有在职用户的部门不可删除） */
    void deleteDept(Long id);
}
