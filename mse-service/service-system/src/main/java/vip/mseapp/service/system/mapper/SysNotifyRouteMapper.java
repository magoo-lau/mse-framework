package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mseapp.service.system.entity.SysNotifyRoute;

/**
 * 通知路由配置 Mapper（租户隔离读写）。
 */
@Mapper
public interface SysNotifyRouteMapper extends BaseMapper<SysNotifyRoute> {
}
