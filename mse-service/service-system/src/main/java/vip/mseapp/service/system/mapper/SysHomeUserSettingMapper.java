package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.system.entity.SysHomeUserSetting;

/** 用户门户首页个性化设置 Mapper。 */
@Mapper
public interface SysHomeUserSettingMapper extends BaseMapper<SysHomeUserSetting> {

    /** 物理删除（绕过逻辑删除：uk_owner 唯一键不能被已删行占位，反复保存/重置场景必须可重建） */
    @Delete("DELETE FROM sys_home_user_setting WHERE owner_type = #{ownerType} AND tenant_id = #{tenantId} AND user_id = #{userId}")
    int deletePhysically(@Param("ownerType") int ownerType, @Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
