package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.OrgDeptDTO;
import vip.mseapp.api.system.dto.OrgRoleDTO;
import vip.mseapp.api.system.dto.OrgUserDTO;
import vip.mseapp.core.result.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * 组织架构查询契约（service-system 实现，只读）。
 *
 * <p>供工作流审批人规则、人员/部门选择器等业务场景消费（如 oa-service 的 OaOrgService
 * 默认桥接实现）。全部方法在调用方租户上下文内执行，经基座租户插件隔离。
 */
public interface RemoteOrgService {

    /** 按编号查询用户 */
    OrgUserDTO getUser(Long userId);

    /** 按编号集合查询用户 */
    List<OrgUserDTO> listUsersByIds(Collection<Long> userIds);

    /** 部门成员；includeChildren=true 时按 sys_dept.ancestors 展开子部门 */
    List<OrgUserDTO> listUsersByDept(Long deptId, boolean includeChildren);

    /** 角色下的用户 */
    List<OrgUserDTO> listUsersByRole(Long roleId);

    /** 全量部门（含 parentId/ancestors，供主管链上溯） */
    List<OrgDeptDTO> listAllDepts();

    /** 人员分页（keyword 模糊匹配用户名/昵称） */
    PageResult<OrgUserDTO> pageUsers(long pageNum, long pageSize, String keyword, Long deptId);

    /** 全量角色 */
    List<OrgRoleDTO> listRoles();

    /** 租户管理员用户编号（首 个 superAdmin 用户；null = 未设置） */
    Long getTenantAdminUserId();
}
