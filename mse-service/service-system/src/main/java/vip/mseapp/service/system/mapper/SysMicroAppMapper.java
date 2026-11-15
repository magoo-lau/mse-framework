package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.system.entity.SysMicroApp;

/** 微前端子应用注册 Mapper。 */
@Mapper
public interface SysMicroAppMapper extends BaseMapper<SysMicroApp> {

    /** 物理清理同 name 逻辑删除墓碑：uk_name 唯一键含墓碑行，不清则同名重建必撞唯一键 */
    @Delete("DELETE FROM sys_micro_app WHERE name = #{name} AND deleted = 1")
    int deleteTombstoneByName(@Param("name") String name);
}
