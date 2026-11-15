package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 附加项定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_addon")
public class SysAddon extends PlatformBaseEntity {

    /** 附加项编码 */
    private String code;
    /** 名称 */
    private String name;
    /** 备注 */
    private String remark;
    /** 0 启用 1 停用 */
    private Integer status;
}
