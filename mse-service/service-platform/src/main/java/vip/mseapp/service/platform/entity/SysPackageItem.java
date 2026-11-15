package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 套餐×权益项（覆盖 default_value） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_package_item")
public class SysPackageItem extends PlatformBaseEntity {

    /** 套餐编号 */
    private Long packageId;
    /** 权益项编号 */
    private Long itemId;
    /** 值 */
    private String itemValue;
}
