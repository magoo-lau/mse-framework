package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调度器设置（平台库全局键值表，「调度器」设置页持久层）：当前承载租户并发闸门上限
 * （{@code gates.<key>=limit}，key=TenantTaskGates.key() 如 data），
 * 保存后写透 Redis 闸门参数键，各实例监控线程控制节拍差量应用（5s 级收敛）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_schedule_setting")
public class SysScheduleSetting extends PlatformBaseEntity {

    /** 设置键（全局唯一；闸门参数约定 gates.&lt;key&gt;） */
    private String settingKey;

    /** 设置值（闸门=每租户并发上限数字串） */
    private String settingValue;

    /** 说明（管理端维护） */
    private String remark;
}
