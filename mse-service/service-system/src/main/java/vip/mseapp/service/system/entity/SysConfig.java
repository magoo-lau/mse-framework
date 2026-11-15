package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数配置（租户隔离表，config_key 租户内唯一）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** config_type：系统内置（仅系统种子可设；业务按键名依赖，不可删除） */
    public static final int SYS_CONFIG_TYPE_BUILT_IN = 0;

    /** config_type：自定义 */
    public static final int SYS_CONFIG_TYPE_CUSTOM = 1;

    /** 参数名称 */
    private String configName;

    /** 参数键名（如 sys.user.initPassword） */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 0 系统内置 1 自定义 */
    private Integer configType;

    private String remark;
}
