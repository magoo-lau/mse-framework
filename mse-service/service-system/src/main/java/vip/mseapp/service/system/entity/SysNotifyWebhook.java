package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.mseapp.mybatis.data.BaseEntity;

/**
 * Webhook 机器人渠道配置（框架内置群发渠道的租户级参数，每世界一行：钉钉/企微/飞书群机器人
 * 地址与报文格式，本租户全部勾选 webhook 的路由共用）。地址清空即渠道不可用（中枢门禁跳过）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_webhook")
public class SysNotifyWebhook extends BaseEntity {

    /** 群机器人 webhook 地址（https 开头；清空=停用渠道） */
    private String webhookUrl;

    /** 报文格式：dingtalk / wecom / feishu（缺省 dingtalk） */
    private String webhookFormat;
}
