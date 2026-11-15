package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.platform.entity.SysTenantAddon;

@Mapper
public interface SysTenantAddonMapper extends BaseMapper<SysTenantAddon> {

    /** 物理删除订购行（纯关联行无审计价值）：uk_tenant_addon 唯一键含逻辑删除墓碑，
     * 退订→重订场景必须物理删，否则同 (tenant_id, addon_id) 再订购必撞唯一键 */
    @Delete("DELETE FROM sys_tenant_addon WHERE tenant_id = #{tenantId} AND addon_id = #{addonId}")
    int deletePhysical(@Param("tenantId") Long tenantId, @Param("addonId") Long addonId);
}
