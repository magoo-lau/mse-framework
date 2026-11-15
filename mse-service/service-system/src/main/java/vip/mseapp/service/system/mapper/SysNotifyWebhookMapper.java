package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mseapp.service.system.entity.SysNotifyWebhook;

/**
 * Webhook 机器人渠道配置 Mapper（租户隔离读写，每世界一行）。
 */
@Mapper
public interface SysNotifyWebhookMapper extends BaseMapper<SysNotifyWebhook> {
}
