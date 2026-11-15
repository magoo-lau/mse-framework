package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 微前端子应用注册（平台级全局表：应用地址类配置 + wujie 加载入口）。
 *
 * <p>name 决定主应用路由前缀 /sub/{name}，prodEntry 须与 nginx 部署路径 /sub-apps/{name}/ 一致；
 * 页面菜单在「菜单管理」以 type=3 子应用页面登记（micro_name 关联本表 name），
 * 未配置页面菜单的子应用在侧边栏渲染为单个菜单项。
 * 租户端可用性由菜单推导：租户菜单集里有该应用任一页面即可用，无页面菜单的应用对所有租户可用。
 * 前端以内置静态注册表兜底：本表无数据时回退，保证零配置起步。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_micro_app")
public class SysMicroApp extends GlobalBaseEntity {

    /** 可见端：仅租户端 */
    public static final int SIDE_TENANT = 1;
    /** 可见端：仅平台端 */
    public static final int SIDE_PLATFORM = 2;
    /** 可见端：双端 */
    public static final int SIDE_BOTH = 3;

    /** 子应用唯一标识（路由前缀 /sub/{name}） */
    private String name;

    /** 显示名（未配置页面菜单时作为侧边栏单项标题） */
    private String title;

    /** 开发环境入口（子应用 dev server，如 //localhost:3400） */
    private String devEntry;

    /** 生产环境入口（静态资源部署路径，如 /sub-apps/oa/） */
    private String prodEntry;

    /** 可见端 1 租户端 2 平台端 3 双端 */
    private Integer side;

    /** 客户端 1 PC 2 移动端（移动基站 mse-ui-mobile 仅加载 client=2 的应用） */
    private Integer client;

    private Integer sort;

    /** 0 启用 1 停用 */
    private Integer status;

    private String remark;
}
