package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysConfig;

/**
 * 参数配置管理（租户隔离，config_key 租户内唯一）。
 *
 * <p>两端独立、单世界生效值：租户会话读「本租户覆盖行 → 本库默认模板行（tenant_id=0 同库副本）」，
 * 平台会话读平台库自身参数行；不跨库合并平台默认。
 */
public interface SysConfigService extends IRepository<SysConfig> {

    PageResult<SysConfig> pageConfigs(long pageNum, long pageSize, String configName, String configKey, Integer configType);

    /** 业务读取入口：按键名取当前世界生效值（本租户覆盖行 → 本库模板行，平台会话即平台库自身行），不存在返回 null */
    String getConfigValueByKey(String configKey);

    Long createConfig(SysConfig config);

    void updateConfig(SysConfig config);

    void deleteConfig(Long id);
}
