package vip.mseapp.service.system.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.mseapp.mybatis.data.BaseEntity;

/**
 * 通知渠道登记目录（渠道提供方自注册的全局目录，登记源 tenant_id=0 行，runIgnore 写；
 * 单体聚合形态本地 Bean 直收不落库）。channel_code 全局唯一，同码不同提供组跳过防覆盖。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_channel")
public class SysNotifyChannel extends BaseEntity {

    /** 渠道编码（全局唯一；inbox 为框架内置，提供方勿占用） */
    private String channelCode;

    /** 展示名 */
    private String channelName;

    /** 渠道说明 */
    private String description;

    /** 是否需要租户授权（true 时租户须持有 entitlement_code 对应权益开关才可用） */
    private Boolean authRequired;

    /** 权益开关编码（auth_required=1 时有效，feature.* 项） */
    private String entitlementCode;

    /** 是否需要个人接收人（NULL/true=按 receiverIds 投递；false=群发渠道如 Webhook，无接收人也投递） */
    private Boolean receiverRequired;

    /** 提供方服务名（dubbo group；本地 Bean 为 local） */
    private String providerGroup;

    /** 最近登记变更时间（描述有变化时刷新） */
    private LocalDateTime lastPushTime;
}
