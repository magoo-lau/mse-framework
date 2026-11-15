package vip.mseapp.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReadyGateFilterTest {

    private static final String NOT_READY_BODY =
            "{\"code\":503,\"message\":\"服务启动中，请稍后重试\",\"data\":null,\"success\":false}";

    private ReadyGateFilter filter(List<String> permitAntPatterns) {
        ReadyGateProperties properties = new ReadyGateProperties();
        properties.setPermitAntPatterns(permitAntPatterns);
        return new ReadyGateFilter(properties);
    }

    @Test
    void blocksBeforeReadyWith503AndRetryAfter() throws Exception {
        ReadyGateFilter filter = filter(List.of("/actuator/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/captcha/new");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("5", response.getHeader("Retry-After"));
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(NOT_READY_BODY, response.getContentAsString());
        assertNull(chain.getRequest(), "未就绪时请求不得进入过滤器链下游");
    }

    @Test
    void passesAfterMarkReadyAndMarkReadyIsIdempotent() throws Exception {
        ReadyGateFilter filter = filter(List.of("/actuator/**"));
        filter.markReady();
        filter.markReady();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/captcha/new");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "开闸后请求应放行到下游");
    }

    @Test
    void permitsActuatorBeforeReady() throws Exception {
        ReadyGateFilter filter = filter(List.of("/actuator/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "/actuator/** 默认放行（探针语义不随闸门 503）");
    }

    @Test
    void permitPatternsAreConfigurable() throws Exception {
        ReadyGateFilter filter = filter(List.of("/public/**"));

        MockHttpServletRequest actuator = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse actuatorResponse = new MockHttpServletResponse();
        MockFilterChain actuatorChain = new MockFilterChain();
        filter.doFilter(actuator, actuatorResponse, actuatorChain);
        assertEquals(503, actuatorResponse.getStatus(), "白名单被配置覆盖后 /actuator 不再放行");
        assertNull(actuatorChain.getRequest());

        MockHttpServletRequest publicReq = new MockHttpServletRequest("GET", "/public/banner");
        MockHttpServletResponse publicResponse = new MockHttpServletResponse();
        MockFilterChain publicChain = new MockFilterChain();
        filter.doFilter(publicReq, publicResponse, publicChain);
        assertEquals(200, publicResponse.getStatus());
        assertNotNull(publicChain.getRequest());
    }
}
