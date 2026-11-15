package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteConfigService;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.service.SysConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数配置查询契约实现：runAs 指定租户语义读取 sys_config（登录策略开关等）。
 *
 * <p>两端独立，单世界生效值：租户 ID = 本租户覆盖行 → 本库默认模板行（tenant_id=0 同库副本，
 * 回退在 {@code SysConfigService.getConfigValueByKey} 内完成）；平台 ID（0）= 平台库自身参数行。
 * 不跨库合并平台默认。
 */
@RpcService(RemoteConfigService.class)
public class RemoteConfigServiceImpl implements RemoteConfigService {

    private final SysConfigService configService;

    public RemoteConfigServiceImpl(SysConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Map<String, String> getConfigValues(Long tenantId, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        TenantContext.runAs(tenantId == null ? MseConstants.PLATFORM_TENANT_ID : tenantId, () -> {
            for (String key : keys) {
                String value = configService.getConfigValueByKey(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
        });
        return result;
    }
}
