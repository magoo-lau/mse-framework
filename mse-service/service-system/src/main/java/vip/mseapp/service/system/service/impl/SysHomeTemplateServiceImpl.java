package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.service.system.entity.SysHomeTemplate;
import vip.mseapp.service.system.mapper.SysHomeTemplateMapper;
import vip.mseapp.service.system.service.SysHomeTemplateService;
import vip.mseapp.service.system.service.support.HomeLayoutJsonValidator;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门户首页模板默认实现：两端物理分库、相互独立，按登录端定位本端行——
 * 平台端=平台库 tenant_id=0 行，租户端=本租户库本租户行；无跨端级联。
 */
public class SysHomeTemplateServiceImpl extends CrudRepository<SysHomeTemplateMapper, SysHomeTemplate>
        implements SysHomeTemplateService {

    @Override
    public TemplateEditVO getForEdit() {
        Long tenantId = currentTenantId();
        SysHomeTemplate row = findByTenant(tenantId);
        return new TemplateEditVO(row != null ? row : empty(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String name, String layoutJson, Integer status) {
        HomeLayoutJsonValidator.validateTemplate(layoutJson);
        Long tenantId = currentTenantId();
        // 物理删除后重建：uk_home_tenant(tenant_id) 唯一键不被逻辑删除行占位
        baseMapper.deletePhysically(tenantId);
        SysHomeTemplate row = new SysHomeTemplate();
        row.setTenantId(tenantId);
        row.setName(name == null || name.isBlank() ? defaultName(tenantId) : name);
        row.setLayoutJson(layoutJson);
        row.setStatus(status == null ? 0 : status);
        save(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset() {
        baseMapper.deletePhysically(currentTenantId());
    }

    @Override
    public SysHomeTemplate resolveEffective() {
        return findEnabled(currentTenantId());
    }

    /** 本端定位：平台端=0（平台库），租户端=当前租户（本租户库） */
    private Long currentTenantId() {
        return UserContext.isPlatform() ? 0L : TenantContext.getRequiredTenantId();
    }

    private SysHomeTemplate findByTenant(Long tenantId) {
        return getOne(new LambdaQueryWrapper<SysHomeTemplate>()
                .eq(SysHomeTemplate::getTenantId, tenantId), false);
    }

    private SysHomeTemplate findEnabled(Long tenantId) {
        SysHomeTemplate row = findByTenant(tenantId);
        return row != null && Integer.valueOf(0).equals(row.getStatus()) ? row : null;
    }

    private SysHomeTemplate empty(Long tenantId) {
        SysHomeTemplate row = new SysHomeTemplate();
        row.setTenantId(tenantId);
        row.setName(defaultName(tenantId));
        row.setStatus(0);
        return row;
    }

    private String defaultName(Long tenantId) {
        return tenantId == 0L ? "平台默认首页" : "租户首页";
    }
}
