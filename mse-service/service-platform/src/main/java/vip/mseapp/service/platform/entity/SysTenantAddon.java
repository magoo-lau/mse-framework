package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户订购附加项 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_addon")
public class SysTenantAddon extends PlatformBaseEntity {

    /** 租户编号 */
    private Long tenantId;
    /** 附加项编号 */
    private Long addonId;
    /** 数量（配额类倍增） */
    private Integer qty;
    /** 0 生效 1 停用 */
    private Integer status;
}
