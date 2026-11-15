package vip.mseapp.service.platform.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 控制面防错库堤坝锁：{@link SysTenantRouteServiceImpl} 整类必须保持 NOT_SUPPORTED 挂起语义。
 *
 * <p>控制面表（sys_tenant_service_config/sys_tenant 等）仅平台库存在，若控制面 SQL 混入调用方
 * 持有的租户路由事务（doBegin 已把连接钉死在租户库），会报
 * 「Table '租户库.sys_tenant_service_config' doesn't exist」并误降级共享缺省路由。
 */
class SysTenantRouteServiceImplSuspensionTest {

    @Test
    void classLevelNotSupportedMustStay() {
        Transactional tx = SysTenantRouteServiceImpl.class.getAnnotation(Transactional.class);
        assertNotNull(tx, "类级 @Transactional(NOT_SUPPORTED) 被移除——控制面 SQL 将混入租户路由事务错库");
        assertEquals(Propagation.NOT_SUPPORTED, tx.propagation(),
                "控制面挂起语义必须是 NOT_SUPPORTED（挂起调用方事务，控制面查询按平台实体钉库执行）");
    }
}
