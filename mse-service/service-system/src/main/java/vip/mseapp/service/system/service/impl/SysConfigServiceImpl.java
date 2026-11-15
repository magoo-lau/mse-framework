package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysConfig;
import vip.mseapp.service.system.mapper.SysConfigMapper;
import vip.mseapp.service.system.service.SysConfigService;
import org.springframework.util.StringUtils;

import java.util.List;

public class SysConfigServiceImpl extends CrudRepository<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public PageResult<SysConfig> pageConfigs(long pageNum, long pageSize, String configName, String configKey, Integer configType) {
        Page<SysConfig> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysConfig>()
                        .like(StringUtils.hasText(configName), SysConfig::getConfigName, configName)
                        .like(StringUtils.hasText(configKey), SysConfig::getConfigKey, configKey)
                        .eq(configType != null, SysConfig::getConfigType, configType)
                        .orderByDesc(SysConfig::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public String getConfigValueByKey(String configKey) {
        // 生效值 = 本租户覆盖行 → 本库默认模板行（tenant_id=0，与租户同库）。
        // 显式携带租户条件（语义与租户插件共享模式一致）：独立库路由下插件跳过 sys_config，
        // 裸按 config_key 查会数到模板行，租户覆盖行出现后多行无法定行。
        Long tenantId = TenantContext.getRequiredTenantId();
        SysConfig own = selectFirstByKey(configKey, tenantId);
        if (own != null || tenantId.longValue() == MseConstants.PLATFORM_TENANT_ID) {
            // 平台域（tenant 0）无模板回退概念：平台库的 tenant_id=0 行即本域行
            return own == null ? null : own.getConfigValue();
        }
        // 租户域默认模板行（各库自持副本，种子"租户可在自己参数里覆盖"的默认源）。
        // 注意必须保持租户上下文原样执行（不能用 runIgnore）：runIgnore 会连数据源路由一起跳过，
        // 查询落到共享主库而非租户库。独立库路由下租户插件本就跳过 sys_config，等值 0 查询直达本库模板行；
        // 共享模式下插件追加本租户条件使 0 行不可见（共享租户在参数设置页也看不到模板行），按无默认处理落代码缺省。
        SysConfig template = selectFirstByKey(configKey, MseConstants.PLATFORM_TENANT_ID);
        return template == null ? null : template.getConfigValue();
    }

    /** 同租户域内按键取行：模板/覆盖行共存或多行脏数据时 getOne 会抛 TooManyResults，列表取首行保证确定性 */
    private SysConfig selectFirstByKey(String configKey, Long tenantId) {
        List<SysConfig> rows = lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getTenantId, tenantId)
                .orderByDesc(SysConfig::getId)
                .list();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public Long createConfig(SysConfig config) {
        checkKeyUnique(config.getConfigKey(), null);
        config.setId(null);
        save(config);
        return config.getId();
    }

    @Override
    public void updateConfig(SysConfig config) {
        checkKeyUnique(config.getConfigKey(), config.getId());
        // 键名建后不允许变更（业务按键名读取，变更等于换配置）
        config.setConfigKey(null);
        updateById(config);
    }

    @Override
    public void deleteConfig(Long id) {
        SysConfig config = getById(id);
        // 系统内置参数（config_type=0）是业务按键名依赖的配置，只允许改值不允许删除
        if (config != null && config.getConfigType() != null && config.getConfigType() == SysConfig.SYS_CONFIG_TYPE_BUILT_IN) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_CONFIG_BUILT_IN);
        }
        removeById(id);
    }

    /**
     * config_key 租户内唯一（平台默认行 tenant_id=0 与租户同键覆盖行合法共存）：显式携带租户条件，
     * 不依赖租户插件的改写——独立库路由下插件跳过 sys_config，缺失该条件会数到库内
     * tenant_id=0 种子模板行，租户侧新建同键覆盖参数被误报「参数键名已存在」。
     */
    private void checkKeyUnique(String configKey, Long excludeId) {
        if (!StringUtils.hasText(configKey)) {
            return;
        }
        Long count = lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getTenantId, TenantContext.getRequiredTenantId())
                .ne(excludeId != null, SysConfig::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_CONFIG_KEY_DUPLICATE);
        }
    }
}
