package vip.mseapp.service.system.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通知投递日志（一次派发一行：计划渠道与逐渠道结果，租户隔离；只写不改）。
 */
@Data
@TableName("sys_notify_log")
public class SysNotifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 业务来源码 */
    private String bizCode;

    /** 消息类型 */
    private String msgType;

    /** 标题（快照） */
    private String title;

    /** 接收人数 */
    private Integer receiverCount;

    /** 计划渠道 CSV */
    private String channels;

    /** 成功渠道 CSV */
    private String okChannels;

    /** 失败渠道 CSV */
    private String failChannels;

    /** 失败原因摘要（截断 500） */
    private String error;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
