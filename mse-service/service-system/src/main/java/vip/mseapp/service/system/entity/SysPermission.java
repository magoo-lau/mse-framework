package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限点（平台级全局表）：API/按钮级权限标识，与导航菜单解耦。
 *
 * <p>menu_id 将权限点挂靠到菜单分组：授权界面按「菜单树 + 每菜单下权限点」勾选，
 * 套餐圈定菜单即隐含圈定其下权限点；menu_id 为 NULL 表示通用权限（如个人资料），不受套餐裁剪。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends GlobalBaseEntity {

    /** 挂靠菜单编号（NULL=通用权限） */
    private Long menuId;

    /** 所属模块：system / tenant / demo */
    private String module;

    /** 权限名 */
    private String name;

    /** 权限标识，如 system:user:create */
    private String code;

    private Integer sort;

    /** 0 启用 1 停用 */
    private Integer status;

    private String remark;
}
