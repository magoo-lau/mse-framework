package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户级权益覆盖（最高优先级） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_entitlement_override")
public class SysTenantEntitlementOverride extends PlatformBaseEntity {

    /** 租户编号 */
    private Long tenantId;
    /** 权益项编号 */
    private Long itemId;
    /** 值 */
    private String itemValue;
}
