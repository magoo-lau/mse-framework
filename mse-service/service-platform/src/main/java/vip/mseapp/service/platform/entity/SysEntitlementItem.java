package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权益项定义（控制面字典：quota.* 配额 / feature.* 开关） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_entitlement_item")
public class SysEntitlementItem extends PlatformBaseEntity {

    /** 权益项编码 */
    private String itemCode;
    /** 1 配额 2 功能开关 */
    private Integer itemType;
    /** 名称 */
    private String name;
    /** INT|BOOL|STRING */
    private String valueType;
    /** 缺省值 */
    private String defaultValue;
    /** 备注 */
    private String remark;
    /** 0 启用 1 停用 */
    private Integer status;
}
