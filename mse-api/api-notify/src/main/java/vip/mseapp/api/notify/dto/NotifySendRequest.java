package vip.mseapp.api.notify.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 通知发送请求：目标渠道 + 系统消息内容。
 *
 * <p>渠道由<b>业务层</b>解析后显式携带（业务各自保存「业务动作 → 渠道」配置）；
 * 为空时中枢默认仅站内信。
 */
public class NotifySendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标渠道编码清单（如 [inbox, im]；空/未携带时默认 [inbox]） */
    private List<String> channels;

    /** 通知内容 */
    private NotifyMessage message;

    public NotifySendRequest() {
    }

    public NotifySendRequest(List<String> channels, NotifyMessage message) {
        this.channels = channels;
        this.message = message;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public NotifyMessage getMessage() {
        return message;
    }

    public void setMessage(NotifyMessage message) {
        this.message = message;
    }
}
