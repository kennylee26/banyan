package cn.kennylee.banyan.openapi.sign.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 可重复读请求包装器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class CachedBodyRequestTest {

    /** 原始请求体。 */
    private static final String BODY = "{\"id\":1}";

    /** 连续两次读取输入流内容一致。 */
    @Test
    void readsBodyTwice() throws Exception {
        // given
        CachedBodyRequestWrapper request = wrapper(BODY);

        // when
        String first = read(request);
        String second = read(request);

        // then
        assertEquals(BODY, first);
        assertEquals(BODY, second);
    }

    /** Reader 与输入流读到相同内容。 */
    @Test
    void readsBodyWithReader() throws Exception {
        // given
        CachedBodyRequestWrapper request = wrapper(BODY);

        // when
        String content;
        try (var reader = request.getReader()) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }

        // then
        assertEquals(BODY, content);
    }

    /** 内容长度与缓存内容一致。 */
    @Test
    void reportsCachedContentLength() throws Exception {
        // given
        CachedBodyRequestWrapper request = wrapper(BODY);

        // when
        int length = request.getContentLength();
        long longLength = request.getContentLengthLong();

        // then
        assertEquals(BODY.getBytes(StandardCharsets.UTF_8).length, length);
        assertEquals(length, longLength);
    }

    /** 写入明文属性后读到明文并更新长度。 */
    @Test
    void replacesBodyWithPlaintext() throws Exception {
        // given
        CachedBodyRequestWrapper request = wrapper(BODY);
        byte[] plaintext = "明文".getBytes(StandardCharsets.UTF_8);

        // when
        request.setAttribute(CachedBodyRequestWrapper.PLAIN_BODY_ATTRIBUTE, plaintext);

        // then
        assertEquals("明文", read(request));
        assertEquals(plaintext.length, request.getContentLength());
    }

    /** 构造包装器并缓存请求体。 */
    private CachedBodyRequestWrapper wrapper(String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new CachedBodyRequestWrapper(request);
    }

    /** 读取请求体。 */
    private String read(CachedBodyRequestWrapper request) throws Exception {
        return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
