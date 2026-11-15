package vip.mseapp.api.notify.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 通知业务来源描述（业务积木自声明；bizCode 全局唯一，如 oa/mes）。
 *
 * <p>来源与消息类型共同构成框架路由配置的匹配维度（bizCode+msgType → 投递渠道，
 * 支持通配）。声明信息随渠道同任务推送至框架目录（幂等刷新），供基座系统管理
 * 「通知渠道设置」页下拉展示；未声明来源的消息仍可发送（路由按通配/兜底解析）。
 */
public class NotifySourceDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务来源码（全局唯一，小写短名，与服务/积木对应，如 oa/mes） */
    private String bizCode;

    /** 展示名（如「协同办公」） */
    private String bizName;

    /** 消息类型清单（来源内动作语义，供配置页下拉） */
    private List<NotifyTypeDescriptor> types;

    public NotifySourceDescriptor() {
    }

    public NotifySourceDescriptor(String bizCode, String bizName, List<NotifyTypeDescriptor> types) {
        this.bizCode = bizCode;
        this.bizName = bizName;
        this.types = types;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getBizName() {
        return bizName;
    }

    public void setBizName(String bizName) {
        this.bizName = bizName;
    }

    public List<NotifyTypeDescriptor> getTypes() {
        return types;
    }

    public void setTypes(List<NotifyTypeDescriptor> types) {
        this.types = types;
    }
}
