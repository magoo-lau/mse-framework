package vip.mseapp.service.system.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.api.system.RemoteBaseDataRegistry;
import vip.mseapp.api.system.dto.BaseScriptDescriptor;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.service.system.service.BaseDataRegistryService;

import java.util.List;

/**
 * 基座库脚本注册契约实现：委托 {@link BaseDataRegistryService} 按账本应用到基座库。
 */
@RpcService(RemoteBaseDataRegistry.class)
public class RemoteBaseDataRegistryImpl implements RemoteBaseDataRegistry {

    private static final Logger log = LoggerFactory.getLogger(RemoteBaseDataRegistryImpl.class);

    private final BaseDataRegistryService baseDataRegistryService;

    public RemoteBaseDataRegistryImpl(BaseDataRegistryService baseDataRegistryService) {
        this.baseDataRegistryService = baseDataRegistryService;
    }

    @Override
    public String register(List<BaseScriptDescriptor> scripts) {
        String summary = baseDataRegistryService.register(scripts);
        log.info("[mse-base-data] 基座库脚本注册: {}", summary.replace("\n", "；"));
        return summary;
    }
}
