package vip.mseapp.rpc.bridge;

import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDatasourceDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.mybatis.datasource.TenantDatasourceDefinition;
import vip.mseapp.mybatis.datasource.TenantDsMetadataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户数据源元数据的 RPC 通道（消费 mse-tenant 的
 * {@link RemoteTenantService#listTenantDatasources}）：业务微服务主库无平台表时的读取实现。
 *
 * <p>password 保持密文传输，由 TenantDsRegistry 按共享 encrypt-key 解密。
 * 基座本地实现在 classpath 时本类不装配（本地 JDBC 通道兜底）。
 */
public class RpcTenantDsMetadataProvider implements TenantDsMetadataProvider {

    @RpcReference
    private RemoteTenantService remoteTenantService;

    @Override
    public List<TenantDatasourceDefinition> listDefinitions(String serviceName) {
        return toDefinitions(remoteTenantService.listTenantDatasources(serviceName));
    }

    @Override
    public TenantDatasourceDefinition getDefinition(String dsKey) {
        return toDefinition(remoteTenantService.getTenantDatasource(dsKey));
    }

    private List<TenantDatasourceDefinition> toDefinitions(List<TenantDatasourceDTO> dtos) {
        List<TenantDatasourceDefinition> list = new ArrayList<>();
        if (dtos == null) {
            return list;
        }
        for (TenantDatasourceDTO dto : dtos) {
            TenantDatasourceDefinition def = toDefinition(dto);
            if (def != null) {
                list.add(def);
            }
        }
        return list;
    }

    private TenantDatasourceDefinition toDefinition(TenantDatasourceDTO dto) {
        if (dto == null) {
            return null;
        }
        TenantDatasourceDefinition def = new TenantDatasourceDefinition();
        def.setId(dto.getId());
        def.setDsKey(dto.getDsKey());
        def.setDsMode(dto.getDsMode());
        def.setServiceName(dto.getServiceName());
        def.setDbType(dto.getDbType());
        def.setHost(dto.getHost());
        def.setPort(dto.getPort());
        def.setDbName(dto.getDbName());
        def.setParams(dto.getParams());
        def.setUsername(dto.getUsername());
        def.setPassword(dto.getPassword());
        def.setStatus(dto.getStatus());
        return def;
    }
}
