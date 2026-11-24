package cn.kennylee.banyan.commons.spring.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 入站 header 读取测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ServletHeadersTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** classpath 具备 Spring Web 时应可用。 */
    @Test
    void reportsAvailable() {
        // when / then
        assertTrue(ServletHeaders.available());
    }

    /** 请求线程内读取入站 header，跳过空值。 */
    @Test
    void readsInboundHeadersAndSkipsEmptyValues() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "t1");
        request.addHeader("X-Empty", "");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when
        Map<String, String> headers = ServletHeaders.inbound();

        // then
        assertEquals("t1", headers.get("X-Tenant"));
        assertFalse(headers.containsKey("X-Empty"));
    }

    /** 没有请求上下文时返回空表。 */
    @Test
    void returnsEmptyOutsideRequest() {
        // when / then
        assertTrue(ServletHeaders.inbound().isEmpty());
    }

    /** 非 Servlet 的请求上下文返回空表。 */
    @Test
    void returnsEmptyForNonServletAttributes() {
        // given
        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));

        // when / then
        assertTrue(ServletHeaders.inbound().isEmpty());
    }

    /** 容器不给 header 名称枚举时返回空表。 */
    @Test
    void returnsEmptyWhenHeaderNamesMissing() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // when / then
        assertTrue(ServletHeaders.inbound().isEmpty());
    }
}
