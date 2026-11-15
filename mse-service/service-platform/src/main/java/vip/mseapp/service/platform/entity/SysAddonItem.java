package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 附加项×权益项 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_addon_item")
public class SysAddonItem extends PlatformBaseEntity {

    /** 附加项编号 */
    private Long addonId;
    /** 权益项编号 */
    private Long itemId;
    /** 值 */
    private String itemValue;
}
