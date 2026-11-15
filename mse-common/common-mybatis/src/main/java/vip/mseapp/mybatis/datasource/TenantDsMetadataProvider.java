package vip.mseapp.mybatis.datasource;

import java.util.Collection;
import java.util.List;

/**
 * 租户数据源元数据提供方（sys_tenant_datasource 的读取通道抽象）。
 *
 * <p>双通道实现（见 docs 多租户分库分表章节）：
 * <ul>
 *   <li>本地 JDBC（common-mybatis 兜底，单体/mse-tenant：平台表在自身主库）；</li>
 *   <li>RPC（starter-bridge，业务微服务：主库无平台表，经 mse-tenant 契约回查，密文传输）。</li>
 * </ul>
 * 返回的定义中密码保持密文，由 TenantDsRegistry 按共享 encrypt-key 解密。
 */
public interface TenantDsMetadataProvider {

    /**
     * 拉取本服务相关的数据源定义（启用 + 停用行均返回，含 service_name ∈ (serviceName, '*')）。
     *
     * @param serviceName 本服务名（spring.application.name 或 '*'）
     */
    List<TenantDatasourceDefinition> listDefinitions(String serviceName);

    /**
     * 多服务名批量拉取（service_name ∈ (serviceNames ∪ {'*'})）：单体聚合场景按
     * {@link TenantServiceModule} 注解收集的模块服务名集合加载各模块限定的数据源。
     * 缺省逐名查询并按 dsKey 去重（'*' 通用行会命中每个名字的查询；RPC 通道零改动）；
     * 本地 JDBC 通道覆写为单条 IN 查询。
     */
    default List<TenantDatasourceDefinition> listDefinitions(Collection<String> serviceNames) {
        java.util.Set<String> distinctNames = new java.util.LinkedHashSet<>();
        for (String serviceName : serviceNames) {
            distinctNames.add(serviceName == null || serviceName.isBlank() ? "*" : serviceName.trim());
        }
        List<TenantDatasourceDefinition> list = new java.util.ArrayList<>();
        java.util.Set<String> seenDsKeys = new java.util.LinkedHashSet<>();
        for (String serviceName : distinctNames) {
            for (TenantDatasourceDefinition def : listDefinitions(serviceName)) {
                if (seenDsKeys.add(String.valueOf(def.getDsKey()))) {
                    list.add(def);
                }
            }
        }
        return list;
    }

    /**
     * 按 dsKey 精确取定义（供变更广播回查判定 upsert/remove）；不存在返回 null。
     */
    TenantDatasourceDefinition getDefinition(String dsKey);
}
