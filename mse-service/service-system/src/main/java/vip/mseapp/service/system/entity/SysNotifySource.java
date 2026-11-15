package vip.mseapp.service.system.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.mseapp.mybatis.data.BaseEntity;

/**
 * 通知业务来源登记目录（业务积木自注册的全局目录，登记源 tenant_id=0 行，runIgnore 写；
 * 单体聚合形态本地 Provider Bean 直收不落库）。biz_code 全局唯一，同码不同提供组跳过防覆盖；
 * types 为消息类型清单 JSON（[{code,name}]，配置页「消息类型」下拉数据源）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_source")
public class SysNotifySource extends BaseEntity {

    /** 业务来源码（全局唯一，如 oa/mes/monitor） */
    private String bizCode;

    /** 展示名（如「协同办公」） */
    private String bizName;

    /** 消息类型清单 JSON（[{code,name}]；可空=来源未声明类型，仅通配可用） */
    private String types;

    /** 提供方服务名（dubbo group；本地 Bean 为 local） */
    private String providerGroup;

    /** 最近登记变更时间（描述有变化时刷新） */
    private LocalDateTime lastPushTime;
}
