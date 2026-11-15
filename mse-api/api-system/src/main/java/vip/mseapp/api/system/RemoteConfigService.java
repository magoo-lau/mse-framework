package vip.mseapp.api.system;

import java.util.List;
import java.util.Map;

/**
 * 参数配置查询契约（service-system 实现）：无库服务（如认证服务 boot-auth）经此读取
 * sys_config 参数（如登录策略开关），实现内部按 tenantId 显式建上下文，登录链路无租户上下文也安全。
 */
public interface RemoteConfigService {

    /**
     * 按租户读取一批参数键值。
     *
     * @param tenantId 租户编号（0=平台域）
     * @param keys     参数键名列表
     * @return key -> value；未配置的键不出现在结果中
     */
    Map<String, String> getConfigValues(Long tenantId, List<String> keys);
}
