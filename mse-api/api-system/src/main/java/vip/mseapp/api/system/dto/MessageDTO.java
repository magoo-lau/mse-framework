package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息视图（RemoteMessageService 契约，字段对应 SysMessage 的接收人视图子集）。
 */
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 消息类型（业务自定义，如 OA 的 TODO/CC/RESULT/REMIND） */
    private String type;

    private String title;

    private String content;

    /** 业务关联标识（如流程实例编号），接收端据此深链 */
    private String bizId;

    /** 0 未读 1 已读 */
    private Integer readFlag;

    private LocalDateTime createTime;

    public MessageDTO() {
    }

    public MessageDTO(Long id, String type, String title, String content, String bizId,
                      Integer readFlag, LocalDateTime createTime) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.bizId = bizId;
        this.readFlag = readFlag;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public Integer getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Integer readFlag) {
        this.readFlag = readFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
