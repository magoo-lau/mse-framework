package vip.mseapp.api.notify.dto;

import java.io.Serializable;

/**
 * 单渠道投递结果（{@code RemoteNotifyService.sendSync} 返回项；ok=false 时 message 为原因摘要）。
 */
public class NotifyChannelOutcome implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 渠道编码 */
    private String channel;

    /** 是否成功 */
    private boolean ok;

    /** 失败原因摘要（成功为 null） */
    private String message;

    public NotifyChannelOutcome() {
    }

    public NotifyChannelOutcome(String channel, boolean ok, String message) {
        this.channel = channel;
        this.ok = ok;
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
