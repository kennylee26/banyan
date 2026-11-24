package cn.kennylee.banyan.commons.spring.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * header 传播规则测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class HeaderRulesTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** 请求线程内读取入站 header。 */
    @Test
    void readsInboundHeaders() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "t1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when
        Map<String, String> headers = HeaderRules.inboundHeaders();

        // then
        assertEquals("t1", headers.get("X-Tenant"));
    }

    /** 没有请求上下文时返回空表。 */
    @Test
    void returnsEmptyOutsideRequest() {
        // when / then
        assertTrue(HeaderRules.inboundHeaders().isEmpty());
    }

    /** 默认阻塞连接与鉴权头，额外阻塞头大小写无关。 */
    @Test
    void removesDefaultAndExtraBlockedHeaders() {
        // given
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("Cookie", "sid=1");
        headers.put("x-tenant", "t1");
        headers.put("X-Keep", "ok");

        // when
        HeaderRules.removeBlocked(headers, List.of("X-TENANT"));

        // then
        assertFalse(headers.containsKey("Authorization"));
        assertFalse(headers.containsKey("Cookie"));
        assertFalse(headers.containsKey("x-tenant"));
        assertEquals(Map.of("X-Keep", "ok"), headers);
    }
}
