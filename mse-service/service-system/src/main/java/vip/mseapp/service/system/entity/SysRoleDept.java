package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-部门关联（租户隔离表）：角色数据范围为「自定义」时的可见部门集合。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_dept")
public class SysRoleDept extends BaseEntity {

    private Long roleId;

    private Long deptId;
}
