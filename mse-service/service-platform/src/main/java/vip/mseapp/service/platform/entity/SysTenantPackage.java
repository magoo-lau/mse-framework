package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户套餐（平台级全局表）：菜单编号集合决定租户可用功能。
 *
 * <p>子应用可用性由菜单集推导（含任一 type=3 页面菜单即可用），无独立的应用级圈定字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class SysTenantPackage extends PlatformBaseEntity {

    private String name;

    /** 可用菜单编号，逗号分隔（M2 起供租户端菜单裁剪使用） */
    private String menuIds;

    private String remark;

    /** 0 启用 1 停用 */
    private Integer status;
}
