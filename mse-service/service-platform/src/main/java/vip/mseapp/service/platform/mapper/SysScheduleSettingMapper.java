package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.service.platform.entity.SysScheduleSetting;
import org.apache.ibatis.annotations.Mapper;

/** 调度器设置表 mapper */
@Mapper
public interface SysScheduleSettingMapper extends BaseMapper<SysScheduleSetting> {
}
