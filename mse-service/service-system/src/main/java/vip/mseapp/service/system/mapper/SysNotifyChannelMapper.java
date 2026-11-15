package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mseapp.service.system.entity.SysNotifyChannel;

/**
 * 通知渠道登记目录 Mapper（登记源 tenant_id=0 行，runIgnore 读写）。
 */
@Mapper
public interface SysNotifyChannelMapper extends BaseMapper<SysNotifyChannel> {
}
