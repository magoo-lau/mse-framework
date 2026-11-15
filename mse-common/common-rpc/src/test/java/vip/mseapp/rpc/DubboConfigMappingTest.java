package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.rpc.dubbo.DubboConfigBinder;
import vip.mseapp.rpc.dubbo.MseDubboSupport;
import vip.mseapp.rpc.fixtures.scan.ScanContract;
import vip.mseapp.rpc.fixtures.scan.ScanContractImpl;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dubbo 全量属性映射：嵌套镜像注解（@RpcService.Dubbo / @RpcReference.Dubbo，
 * @DubboService/@DubboReference 全属性拷贝）→ 原生 ServiceConfig/ReferenceConfig/MethodConfig
 * （不触发真实导出/注册中心连接）。
 */
class DubboConfigMappingTest {

    static class RefFixtures {
        @RpcReference(dubbo = @RpcReference.Dubbo(version = "v1", group = "g1", timeout = 7000, retries = 3,
                loadbalance = "leastactive", cluster = "failfast", actives = 10, sticky = true,
                url = "127.0.0.1:20880", filter = {"mseContext", "custom"},
                parameters = {"k1=v1", "k2=v2"},
                methods = {@Method(name = "query", timeout = 3000, oninvoke = "cbHook.before",
                        arguments = {@Argument(index = 0, type = "java.lang.Long")})}))
        Object full;

        @RpcReference
        Object plain;
    }

    @RpcService(value = ScanContract.class, dubbo = @RpcService.Dubbo(version = "9.9", group = "mse",
            timeout = 8000, retries = 2, loadbalance = "roundrobin", weight = 200, executes = 16,
            token = "abc", delay = 500, parameters = {"a=1"},
            methods = {@Method(name = "name", timeout = 4321)}))
    static class DubboAnnotatedImpl implements ScanContract {
        @Override
        public String name() {
            return "dubbo";
        }
    }

    private static RpcReference.Dubbo refDubbo(String fieldName) throws Exception {
        return AnnotatedElementUtils.findMergedAnnotation(
                RefFixtures.class.getDeclaredField(fieldName), RpcReference.class).dubbo();
    }

    @Test
    void referenceFullAttributesApplied() throws Exception {
        ReferenceConfig<Object> config = new ReferenceConfig<>();
        DubboConfigBinder.bindReference(config, refDubbo("full"));
        assertEquals("v1", config.getVersion());
        assertEquals("g1", config.getGroup());
        assertEquals(Integer.valueOf(7000), config.getTimeout());
        assertEquals(Integer.valueOf(3), config.getRetries());
        assertEquals("leastactive", config.getLoadbalance());
        assertEquals("failfast", config.getCluster());
        assertEquals(Integer.valueOf(10), config.getActives());
        assertEquals(Boolean.TRUE, config.getSticky());
        assertEquals("127.0.0.1:20880", config.getUrl());
        assertTrue(String.join(",", String.valueOf(config.getFilter()).replaceAll("\\[|\\]", "")
                .split(",")).contains("mseContext"), "filter 应落逗号串并包含 mseContext");
        assertEquals("v1", config.getParameters().get("k1"));
        assertEquals("v2", config.getParameters().get("k2"));
    }

    @Test
    void referenceMethodLevelAndCallbackSplit() throws Exception {
        ReferenceConfig<Object> config = new ReferenceConfig<>();
        DubboConfigBinder.bindReference(config, refDubbo("full"));
        assertEquals(1, config.getMethods().size());
        MethodConfig method = config.getMethods().get(0);
        assertEquals("query", method.getName());
        assertEquals(Integer.valueOf(3000), method.getTimeout());
        // "cbHook.before" 拆分为实例段 + 方法段（与 Dubbo 注解处理一致）
        assertEquals("cbHook", method.getOninvoke());
        assertEquals("before", method.getOninvokeMethod());
        assertEquals(1, method.getArguments().size());
        ArgumentConfig argument = method.getArguments().get(0);
        assertEquals(Integer.valueOf(0), argument.getIndex());
        assertEquals("java.lang.Long", argument.getType());
    }

    @Test
    void referenceDefaultsPreserveRepoBehavior() throws Exception {
        // 未设置任何属性：保留仓内两处有意缺省——timeout=5000、check=false（与 Dubbo 原注解 -1/true 不同）
        ReferenceConfig<Object> config = new ReferenceConfig<>();
        DubboConfigBinder.bindReference(config, refDubbo("plain"));
        assertEquals(Integer.valueOf(5000), config.getTimeout());
        assertEquals(Boolean.FALSE, config.isCheck());
        assertNull(config.getVersion());
        assertNull(config.getGroup());
        assertNull(config.getRetries());
    }

    @Test
    void serviceFullAttributesApplied() {
        RpcService rpcService = AnnotatedElementUtils.findMergedAnnotation(DubboAnnotatedImpl.class, RpcService.class);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("environment", testEnvironment());
        ServiceConfig<ScanContract> config = MseDubboSupport.serviceConfig(
                ScanContract.class, new ScanContractImpl(), beanFactory, rpcService.dubbo());
        assertEquals("9.9", config.getVersion());
        assertEquals("mse", config.getGroup());
        assertEquals(Integer.valueOf(8000), config.getTimeout());
        assertEquals(Integer.valueOf(2), config.getRetries());
        assertEquals("roundrobin", config.getLoadbalance());
        assertEquals(Integer.valueOf(200), config.getWeight());
        assertEquals(Integer.valueOf(16), config.getExecutes());
        assertEquals("abc", config.getToken());
        assertEquals(Integer.valueOf(500), config.getDelay());
        assertEquals("1", config.getParameters().get("a"));
        assertEquals(1, config.getMethods().size());
        assertEquals("name", config.getMethods().get(0).getName());
        assertEquals(Integer.valueOf(4321), config.getMethods().get(0).getTimeout());
    }

    @Test
    void serviceConfigWithoutDubboKeepsDefaults() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("environment", testEnvironment());
        ServiceConfig<ScanContract> config = MseDubboSupport.serviceConfig(
                ScanContract.class, new ScanContractImpl(), beanFactory);
        assertNull(config.getVersion());
        assertNull(config.getGroup());
        assertNull(config.getRetries());
        assertNull(config.getTimeout());
    }

    private StandardEnvironment testEnvironment() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test",
                java.util.Map.of("dubbo.application.name", "common-rpc-test")));
        return env;
    }
}
