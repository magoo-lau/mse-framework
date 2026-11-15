package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.platform.entity.SysAddon;

@Mapper
public interface SysAddonMapper extends BaseMapper<SysAddon> {

    /** 物理清理同 code 逻辑删除墓碑：uk_addon_code 唯一键含墓碑行，不清则同编码重建必撞唯一键 */
    @Delete("DELETE FROM sys_addon WHERE code = #{code} AND deleted = 1")
    int deleteTombstoneByCode(@Param("code") String code);
}
