package cn.kennylee.banyan.openapi.sign.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 可重复读过滤器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RepeatableBodyFilterTest {

    /** 待测过滤器。 */
    private final RepeatableBodyFilter filter = new RepeatableBodyFilter();

    /** JSON 请求被包装。 */
    @Test
    void wrapsJsonRequest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openapi/orders");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertInstanceOf(CachedBodyRequestWrapper.class, chain.getRequest());
    }

    /** 文件上传不缓存。 */
    @Test
    void skipsMultipartRequest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload");
        request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        request.setContent(new byte[]{1, 2});
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertSame(request, chain.getRequest());
    }

    /** 已缓存的请求不重复包装。 */
    @Test
    void doesNotWrapTwice() throws Exception {
        // given
        MockHttpServletRequest original = new MockHttpServletRequest("POST", "/openapi/orders");
        original.setContent("{}".getBytes(StandardCharsets.UTF_8));
        CachedBodyRequestWrapper wrapped = new CachedBodyRequestWrapper(original);
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(wrapped, new MockHttpServletResponse(), chain);

        // then
        assertSame(wrapped, chain.getRequest());
    }
}
