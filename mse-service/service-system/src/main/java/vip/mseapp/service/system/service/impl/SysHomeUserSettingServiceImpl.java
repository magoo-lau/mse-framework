package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.service.system.entity.SysHomeUserSetting;
import vip.mseapp.service.system.mapper.SysHomeUserSettingMapper;
import vip.mseapp.service.system.service.SysHomeUserSettingService;
import vip.mseapp.service.system.service.support.HomeLayoutJsonValidator;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户门户首页个性化设置默认实现：按登录端归属（owner_type）+ 当前用户定位唯一一行。
 */
public class SysHomeUserSettingServiceImpl extends CrudRepository<SysHomeUserSettingMapper, SysHomeUserSetting>
        implements SysHomeUserSettingService {

    @Override
    public SysHomeUserSetting getMySetting() {
        return getOne(new LambdaQueryWrapper<SysHomeUserSetting>()
                .eq(SysHomeUserSetting::getOwnerType, ownerType())
                .eq(SysHomeUserSetting::getTenantId, tenantId())
                .eq(SysHomeUserSetting::getUserId, UserContext.getUserId()), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMySetting(String layoutJson) {
        HomeLayoutJsonValidator.validateUserSetting(layoutJson);
        // 物理删除后重建：uk_owner(owner_type, tenant_id, user_id) 唯一键不被逻辑删除行占位
        baseMapper.deletePhysically(ownerType(), tenantId(), UserContext.getUserId());
        SysHomeUserSetting row = new SysHomeUserSetting();
        row.setOwnerType(ownerType());
        row.setTenantId(tenantId());
        row.setUserId(UserContext.getUserId());
        row.setLayoutJson(layoutJson);
        save(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetMySetting() {
        baseMapper.deletePhysically(ownerType(), tenantId(), UserContext.getUserId());
    }

    private int ownerType() {
        return UserContext.isPlatform() ? SysHomeUserSetting.OWNER_PLATFORM : SysHomeUserSetting.OWNER_TENANT;
    }

    private Long tenantId() {
        return UserContext.isPlatform() ? 0L : TenantContext.getRequiredTenantId();
    }
}
