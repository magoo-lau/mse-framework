package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导航菜单（平台级全局表：由平台统一维护，纯前端路由职责）。
 *
 * <p>权限标识已拆分到独立的 {@link SysPermission}（权限点挂靠菜单，授权界面按菜单分组展示）；
 * menu_scope 划分平台端/租户端/双端三个可见域，套餐只能圈定租户域菜单。
 * type=3 子应用页面：micro_name 关联 {@link SysMicroApp}，path 为子应用内部路由，
 * 由基座侧边栏解析为 /sub/{name}{path}，随菜单树参与角色授权与套餐裁剪。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends GlobalBaseEntity {

    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 1 目录 2 菜单 3 子应用页面 */
    private Integer type;

    /** 可见域 1 平台端 2 租户端 3 双端 */
    private Integer menuScope;

    /** 客户端 1 PC 2 移动端（菜单是"入口"按端分离；权限点是"能力"双端共享，见 SysPermission） */
    private Integer client;

    /** 前端路由地址（type=3 为子应用内部路由，支持 :param 动态段） */
    private String path;

    /** 所属子应用标识（type=3 专用，关联 sys_micro_app.name） */
    private String microName;

    /** 前端组件路径 */
    private String component;

    private String icon;

    private Integer sort;

    /** 0 显示 1 隐藏 */
    private Integer visible;

    /** 0 启用 1 停用 */
    private Integer status;

    /** 类型：目录 */
    public static final int TYPE_DIR = 1;

    /** 类型：菜单 */
    public static final int TYPE_MENU = 2;

    /** 类型：子应用页面 */
    public static final int TYPE_MICRO = 3;

    /** 可见域：平台端 */
    public static final int SCOPE_PLATFORM = 1;

    /** 可见域：租户端 */
    public static final int SCOPE_TENANT = 2;

    /** 可见域：双端 */
    public static final int SCOPE_BOTH = 3;

    /** 客户端：PC（存量缺省，零迁移） */
    public static final int CLIENT_PC = 1;

    /** 客户端：移动端 */
    public static final int CLIENT_MOBILE = 2;
}
