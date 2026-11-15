package vip.mseapp.api.notify.dto;

import java.io.Serializable;

/**
 * 通知消息类型描述（业务动作语义，归属于某个业务来源；如 oa 的 TODO 待办/CC 抄送）。
 *
 * <p>类型编码在所属来源内唯一即可（路由按 bizCode+msgType 二元组匹配）；展示名供
 * 框架配置页「消息类型」下拉显示。未登记的类型仍可发送（路由按通配/兜底解析），
 * 只是配置页不提供下拉项。
 */
public class NotifyTypeDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类型编码（来源内唯一，大写风格，如 TODO/CC/ALERT） */
    private String code;

    /** 展示名（如「待办」） */
    private String name;

    public NotifyTypeDescriptor() {
    }

    public NotifyTypeDescriptor(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
