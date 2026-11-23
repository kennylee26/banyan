package cn.kennylee.banyan.spring.feign.interceptor;

import cn.kennylee.banyan.spring.feign.FeignContextHolder;
import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * header 转发拦截器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ForwardHeadersInterceptorTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
        FeignContextHolder.clear();
    }

    /** 转发入站 header 并应用默认阻塞名单。 */
    @Test
    void forwardsHeadersAndAppliesBlockList() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "t1");
        request.addHeader("Authorization", "Bearer secret");
        request.addHeader("Cookie", "sid=1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ForwardHeadersInterceptor interceptor = new ForwardHeadersInterceptor(new FeignProperties());
        RequestTemplate template = new RequestTemplate();

        // when
        interceptor.apply(template);

        // then
        assertEquals("t1", template.headers().get("X-Tenant").iterator().next());
        assertFalse(template.headers().containsKey("Authorization"));
        assertFalse(template.headers().containsKey("Cookie"));
    }

    /** 单次禁用转发后不写任何 header。 */
    @Test
    void skipsForwardingWhenDisabledPerCall() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "t1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        FeignContextHolder.disableForwardHeaders();
        ForwardHeadersInterceptor interceptor = new ForwardHeadersInterceptor(new FeignProperties());
        RequestTemplate template = new RequestTemplate();

        // when
        interceptor.apply(template);

        // then
        assertTrue(template.headers().isEmpty());
    }

    /** 默认 apply 后清理上下文；关闭自动清理时保留。 */
    @Test
    void clearsContextAfterApplyByDefault() {
        // given
        FeignContextHolder.addCustomHeader("X-Tenant", "t1");
        ForwardHeadersInterceptor interceptor = new ForwardHeadersInterceptor(new FeignProperties());

        // when
        interceptor.apply(new RequestTemplate());

        // then
        assertTrue(FeignContextHolder.current().getCustomHeaders().isEmpty());
    }

    /** 自定义头覆盖，阻塞头大小写无关。 */
    @Test
    void appliesCustomHeadersAndCaseInsensitiveBlockList() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-tenant", "origin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        FeignContextHolder.addBlockedHeaders(List.of("X-TENANT"));
        FeignContextHolder.addCustomHeader("X-Tenant", "override");
        ForwardHeadersInterceptor interceptor = new ForwardHeadersInterceptor(new FeignProperties());
        RequestTemplate template = new RequestTemplate();

        // when
        interceptor.apply(template);

        // then
        assertEquals("override", template.headers().get("X-Tenant").iterator().next());
    }
}
