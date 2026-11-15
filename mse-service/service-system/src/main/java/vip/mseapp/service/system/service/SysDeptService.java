package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysDept;

import java.util.List;

/**
 * 部门管理（树形；租户端与平台端各自维护本域部门树，数据隔离由租户插件保证）。
 */
public interface SysDeptService extends IRepository<SysDept> {

    /** 当前域部门树 */
    List<DeptTreeNode> getDeptTree();

    Long createDept(Long parentId, String name, Integer sort);

    Long createDept(Long parentId, String name, Integer sort, Long leaderUserId);

    /** parentId 变更时级联刷新子树 ancestors */
    void updateDept(Long id, Long parentId, String name, Integer sort, Integer status);

    /**
     * leaderUserId：null=不改动（兼容旧签名调用方）、0=清空、其余=负责人编号（须为本域启用用户）。
     */
    void updateDept(Long id, Long parentId, String name, Integer sort, Integer status, Long leaderUserId);

    /** 有子部门或有在职用户的部门不可删除 */
    void deleteDept(Long id);

    record DeptTreeNode(SysDept dept, List<DeptTreeNode> children, String leaderName) {
    }
}
