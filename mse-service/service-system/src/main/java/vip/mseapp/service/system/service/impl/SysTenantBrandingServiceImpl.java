package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysTenantBranding;
import vip.mseapp.service.system.mapper.SysTenantBrandingMapper;
import vip.mseapp.service.system.service.SysTenantBrandingService;
import vip.mseapp.service.system.service.TenantBrandingCachePublisher;

public class SysTenantBrandingServiceImpl extends CrudRepository<SysTenantBrandingMapper, SysTenantBranding>
        implements SysTenantBrandingService {

    private final ObjectProvider<TenantBrandingCachePublisher> publisherProvider;

    public SysTenantBrandingServiceImpl(ObjectProvider<TenantBrandingCachePublisher> publisherProvider) {
        // 惰性解析发布器：发布器反向依赖本服务，直接注入会构成装配环
        this.publisherProvider = publisherProvider;
    }

    @Override
    public SysTenantBranding getOwn() {
        return selectByTenantId(TenantContext.getRequiredTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOwn(SysTenantBranding branding) {
        Long tenantId = TenantContext.getRequiredTenantId();
        SysTenantBranding own = selectByTenantId(tenantId);
        if (own == null) {
            own = new SysTenantBranding();
            // tenant_id 由 MetaObjectHandler 按当前租户上下文自动填充
            own.setSiteTitle(blankToNull(branding.getSiteTitle()));
            own.setSiteLogo(blankToNull(branding.getSiteLogo()));
            own.setLoginBackground(blankToNull(branding.getLoginBackground()));
            own.setBackgroundColor(blankToNull(branding.getBackgroundColor()));
            own.setCopyright(blankToNull(branding.getCopyright()));
            save(own);
        } else {
            // 显式 set 全部字段（updateById 忽略 null 无法表达"清空"），空白归一为 null
            lambdaUpdate()
                    .eq(SysTenantBranding::getId, own.getId())
                    .set(SysTenantBranding::getSiteTitle, blankToNull(branding.getSiteTitle()))
                    .set(SysTenantBranding::getSiteLogo, blankToNull(branding.getSiteLogo()))
                    .set(SysTenantBranding::getLoginBackground, blankToNull(branding.getLoginBackground()))
                    .set(SysTenantBranding::getBackgroundColor, blankToNull(branding.getBackgroundColor()))
                    .set(SysTenantBranding::getCopyright, blankToNull(branding.getCopyright()))
                    .update();
        }
        // 写穿：该租户名下全部绑定域名的品牌条目即时刷新（登录页免等重建）。
        // 仅同世界读 + Redis 写，Redis 异常不回滚品牌保存（读取侧 DB 兜底自愈）
        try {
            TenantBrandingCachePublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.publishTenant(tenantId);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(SysTenantBrandingServiceImpl.class)
                    .warn("[mse-branding-cache] 品牌保存后缓存写穿失败（DB 已保存）: {}", ex.getMessage());
        }
    }

    @Override
    public SysTenantBranding getByTenantId(Long tenantId) {
        return selectByTenantId(tenantId);
    }

    /** 显式携带 tenant 条件：独立库路由下租户插件跳过本表，裸查会读到错误世界 */
    private SysTenantBranding selectByTenantId(Long tenantId) {
        return lambdaQuery()
                .eq(SysTenantBranding::getTenantId, tenantId)
                .one();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
