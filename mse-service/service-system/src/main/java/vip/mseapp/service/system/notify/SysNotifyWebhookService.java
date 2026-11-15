package vip.mseapp.service.system.notify;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysNotifyWebhook;
import vip.mseapp.service.system.mapper.SysNotifyWebhookMapper;

/**
 * Webhook 机器人渠道配置（租户级，每世界一行）：内置 webhook 渠道的地址与报文格式读写，
 * 在「通知渠道设置」路由勾选处内联维护，本租户全部勾选 webhook 的路由共用。
 */
@Service
public class SysNotifyWebhookService {

    /** 支持的群机器人报文格式（钉钉/企业微信/飞书） */
    public static final Set<String> FORMATS = Set.of("dingtalk", "wecom", "feishu");

    private final SysNotifyWebhookMapper webhookMapper;

    public SysNotifyWebhookService(SysNotifyWebhookMapper webhookMapper) {
        this.webhookMapper = webhookMapper;
    }

    /** 当前世界配置（未配置返回缺省空地址/dingtalk，即渠道不可用） */
    public SysNotifyWebhook current() {
        SysNotifyWebhook row = webhookMapper.selectOne(new LambdaQueryWrapper<SysNotifyWebhook>()
                .last("LIMIT 1"));
        if (row == null) {
            row = new SysNotifyWebhook();
            row.setWebhookUrl("");
            row.setWebhookFormat("dingtalk");
        }
        return row;
    }

    /** 保存（每世界 upsert 一行）：地址可清空（=停用），非空时必须 http(s) 开头 */
    public void save(String webhookUrl, String webhookFormat) {
        String url = webhookUrl == null ? "" : webhookUrl.trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ServiceException(400, "Webhook 地址必须以 http(s):// 开头");
        }
        String format = webhookFormat == null || webhookFormat.isBlank() ? "dingtalk" : webhookFormat.trim();
        if (!FORMATS.contains(format)) {
            throw new ServiceException(400, "报文格式仅支持：" + String.join("/", FORMATS));
        }
        SysNotifyWebhook row = current();
        row.setWebhookUrl(url);
        row.setWebhookFormat(format);
        if (row.getId() == null) {
            webhookMapper.insert(row);
        } else {
            webhookMapper.updateById(row);
        }
    }
}
