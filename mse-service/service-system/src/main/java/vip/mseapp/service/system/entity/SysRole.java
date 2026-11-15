package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色（租户隔离表，code 租户内唯一；tenant_id=0 为平台角色）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String name;

    private String code;

    private Integer sort;

    /** 数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人 */
    private Integer dataScope;

    /** 0 启用 1 停用 */
    private Integer status;

    private String remark;

    /** 数据范围：全部 */
    public static final int SCOPE_ALL = 1;

    /** 数据范围：自定义部门（配合 sys_role_dept） */
    public static final int SCOPE_CUSTOM = 2;

    /** 数据范围：本部门 */
    public static final int SCOPE_DEPT = 3;

    /** 数据范围：本部门及以下 */
    public static final int SCOPE_DEPT_AND_CHILD = 4;

    /** 数据范围：仅本人 */
    public static final int SCOPE_SELF = 5;
}
