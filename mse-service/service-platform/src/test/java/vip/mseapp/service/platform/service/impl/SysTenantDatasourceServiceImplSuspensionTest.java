package vip.mseapp.service.platform.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.service.platform.service.support.TenantDatasourceBatchSyncService;
import vip.mseapp.service.platform.service.support.TenantDatasourceConnectionTester;
import vip.mseapp.service.platform.service.support.TenantDatasourceInitializer;
import vip.mseapp.service.platform.service.support.TenantSchemaOpsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 控制面防错库堤坝锁：SysTenantDatasourceServiceImpl 门面及其拆分出的全部协作者
 * 必须保持类级 NOT_SUPPORTED 挂起语义（拆分/演进时新增的控制面类同样要挂进来锁定）。
 *
 * <p>控制面表（sys_tenant_datasource 等）仅平台库存在，若控制面 SQL 混入调用方持有的
 * 租户路由事务（doBegin 已把连接钉死在租户库），会报缺表并误降级路由。
 * 协作者经门面 @Bean 注册同样会被业务调用方事务包围，堤坝语义与门面一致。
 */
class SysTenantDatasourceServiceImplSuspensionTest {

    @Test
    void controlPlaneClassesMustStayNotSupported() {
        assertNotSupported(SysTenantDatasourceServiceImpl.class);
        assertNotSupported(TenantDatasourceConnectionTester.class);
        assertNotSupported(TenantDatasourceInitializer.class);
        assertNotSupported(TenantSchemaOpsService.class);
        assertNotSupported(TenantDatasourceBatchSyncService.class);
    }

    private static void assertNotSupported(Class<?> clazz) {
        Transactional tx = clazz.getAnnotation(Transactional.class);
        assertNotNull(tx, clazz.getSimpleName()
                + " 类级 @Transactional(NOT_SUPPORTED) 被移除——控制面 SQL 将混入租户路由事务错库");
        assertEquals(Propagation.NOT_SUPPORTED, tx.propagation(), clazz.getSimpleName()
                + " 控制面挂起语义必须是 NOT_SUPPORTED（挂起调用方事务，控制面查询按平台实体钉库执行）");
    }
}
