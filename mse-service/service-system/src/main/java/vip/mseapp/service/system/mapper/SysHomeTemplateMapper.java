package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.system.entity.SysHomeTemplate;

/** 门户首页模板 Mapper。 */
@Mapper
public interface SysHomeTemplateMapper extends BaseMapper<SysHomeTemplate> {

    /** 物理删除（绕过逻辑删除：uk_home_tenant 唯一键不能被已删行占位，保存/恢复默认需可反复重建） */
    @Delete("DELETE FROM sys_home_template WHERE tenant_id = #{tenantId}")
    int deletePhysically(@Param("tenantId") Long tenantId);
}
