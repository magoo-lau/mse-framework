package vip.mseapp.service.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.platform.entity.SysTenantEntitlementOverride;

@Mapper
public interface SysTenantEntitlementOverrideMapper extends BaseMapper<SysTenantEntitlementOverride> {

    /** 物理删除覆盖行（纯开关行无审计价值）：uk_tenant_item 唯一键含逻辑删除墓碑，
     *  清除→重存场景必须物理删，否则同 (tenant_id, item_id) 再保存必撞唯一键 */
    @Delete("DELETE FROM sys_tenant_entitlement_override WHERE tenant_id = #{tenantId} AND item_id = #{itemId}")
    int deletePhysical(@Param("tenantId") Long tenantId, @Param("itemId") Long itemId);
}
