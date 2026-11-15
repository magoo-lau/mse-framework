package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息（租户隔离，接收人个人数据；写入来自 RemoteMessageService 契约投递，
 * 不继承 BaseEntity——表无 update_by/update_time 列，仅已读标记可更新）。
 */
@Data
@TableName("sys_message")
public class SysMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 消息类型（业务自定义，如 OA 的 TODO/CC/RESULT/REMIND） */
    private String type;

    /** 接收人 */
    private Long receiverId;

    private String title;

    private String content;

    /** 业务关联标识（如流程实例编号） */
    private String bizId;

    /** 0 未读 1 已读 */
    private Integer readFlag;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
