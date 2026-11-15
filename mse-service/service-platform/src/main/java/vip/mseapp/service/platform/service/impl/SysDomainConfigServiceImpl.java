package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.service.platform.entity.SysDomainConfig;
import vip.mseapp.service.platform.mapper.SysDomainConfigMapper;
import vip.mseapp.service.platform.runner.DomainBrandingCacheRunner;
import vip.mseapp.service.platform.service.SysDomainConfigService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

// 控制面服务（sys_domain_config 仅平台库存在）：类级 NOT_SUPPORTED 防错库堤坝，勿删
//（范式详见 SysTenantRouteServiceImpl 类注释）。
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SysDomainConfigServiceImpl extends CrudRepository<SysDomainConfigMapper, SysDomainConfig>
        implements SysDomainConfigService {

    /** 域名格式：完整主机名（至少含一个点），与租户绑定域名口径一致 */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$");

    private final ObjectProvider<DomainBrandingCache> cacheProvider;

    public SysDomainConfigServiceImpl(ObjectProvider<DomainBrandingCache> cacheProvider) {
        // 平台自有域名的增删改即时写穿缓存（登录页解析只读缓存，DB 只是真相源）
        this.cacheProvider = cacheProvider;
    }

    @Override
    public PageResult<SysDomainConfig> pageDomainConfigs(long pageNum, long pageSize, String domain, Integer status) {
        Page<SysDomainConfig> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysDomainConfig>()
                        .like(StringUtils.hasText(domain), SysDomainConfig::getDomain, domain)
                        .eq(status != null, SysDomainConfig::getStatus, status)
                        .orderByDesc(SysDomainConfig::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public Long createDomainConfig(SysDomainConfig config) {
        String domain = normalizeDomain(config.getDomain());
        checkDomainUnique(domain, null);
        config.setId(null);
        config.setDomain(domain);
        // 与 update 通道同口径：空白归一为 null（校验放行空串后此处是空串进库的唯一入口）
        config.setSiteTitle(blankToNull(config.getSiteTitle()));
        config.setSiteLogo(blankToNull(config.getSiteLogo()));
        config.setLoginBackground(blankToNull(config.getLoginBackground()));
        config.setBackgroundColor(blankToNull(config.getBackgroundColor()));
        config.setCopyright(blankToNull(config.getCopyright()));
        config.setRemark(blankToNull(config.getRemark()));
        config.setStatus(config.getStatus() == null ? 0 : config.getStatus());
        save(config);
        writeThrough(config.getDomain(), config.getStatus());
        return config.getId();
    }

    @Override
    public void updateDomainConfig(SysDomainConfig config) {
        SysDomainConfig existing = config.getId() == null ? null : getById(config.getId());
        if (existing == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "配置不存在");
        }
        int status = config.getStatus() == null ? 0 : config.getStatus();
        // updateById 忽略 null 字段，无法表达"清空"；个性化字段统一显式 set，空白归一为 null
        lambdaUpdate()
                .eq(SysDomainConfig::getId, config.getId())
                .set(SysDomainConfig::getSiteTitle, blankToNull(config.getSiteTitle()))
                .set(SysDomainConfig::getSiteLogo, blankToNull(config.getSiteLogo()))
                .set(SysDomainConfig::getLoginBackground, blankToNull(config.getLoginBackground()))
                .set(SysDomainConfig::getBackgroundColor, blankToNull(config.getBackgroundColor()))
                .set(SysDomainConfig::getCopyright, blankToNull(config.getCopyright()))
                .set(SysDomainConfig::getRemark, blankToNull(config.getRemark()))
                .set(SysDomainConfig::getStatus, status)
                .update();
        writeThrough(existing.getDomain(), status);
    }

    @Override
    public void deleteDomainConfig(Long id) {
        SysDomainConfig existing = getById(id);
        removeById(id);
        if (existing != null) {
            writeThrough(existing.getDomain(), 1);
        }
    }

    @Override
    public SysDomainConfig getEnabledByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return lambdaQuery()
                .eq(SysDomainConfig::getDomain, domain.trim().toLowerCase())
                .eq(SysDomainConfig::getStatus, 0)
                .one();
    }

    /** 域名缓存写穿：启用即写入/刷新条目，停用或删除即摘除；Redis 异常不影响管理端操作 */
    private void writeThrough(String domain, int status) {
        try {
            DomainBrandingCache cache = cacheProvider.getIfAvailable();
            if (cache == null) {
                return;
            }
            if (status == 0) {
                // 以库内最新行装配条目（写穿与落库保持一致；并发删除时条目按消失处理）
                SysDomainConfig latest = getEnabledByDomain(domain);
                if (latest != null) {
                    cache.putPlatformDomain(domain, DomainBrandingCacheRunner.toEntry(latest));
                } else {
                    cache.evictPlatformDomain(domain);
                }
            } else {
                cache.evictPlatformDomain(domain);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(SysDomainConfigServiceImpl.class)
                    .warn("[mse-branding-cache] 平台域名缓存写穿失败（DB 已保存）: {}", ex.getMessage());
        }
    }

    /** 规范化域名：trim + 小写；格式非法抛 400 */
    private String normalizeDomain(String domain) {
        String normalized = domain == null ? "" : domain.trim().toLowerCase();
        if (!DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "域名格式不正确（完整主机名，如 demo.example.com）");
        }
        return normalized;
    }

    /** 域名唯一性校验；excludeId 用于更新场景（当前域名不可改，仅防御并发） */
    private void checkDomainUnique(String domain, Long excludeId) {
        Long count = lambdaQuery()
                .eq(SysDomainConfig::getDomain, domain)
                .ne(excludeId != null, SysDomainConfig::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该域名已存在个性化配置");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
