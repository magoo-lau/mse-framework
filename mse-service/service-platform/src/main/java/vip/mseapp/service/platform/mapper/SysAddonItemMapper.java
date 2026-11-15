package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.platform.entity.SysAddonItem;

@Mapper
public interface SysAddonItemMapper extends BaseMapper<SysAddonItem> {

    /** 物理删除附加项明细（纯关联行无审计价值）：uk_addon_item 唯一键含逻辑删除墓碑，
     * 全删重插场景必须物理删，否则同 (addon_id, item_id) 重建必撞唯一键 */
    @Delete("DELETE FROM sys_addon_item WHERE addon_id = #{addonId}")
    int deletePhysicalByAddonId(@Param("addonId") Long addonId);
}
