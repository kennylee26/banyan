package cn.kennylee.banyan.openapi.sign.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 统一错误响应与 JSON 转义测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ErrorResponsesTest {

    /** 空值转义为空串。 */
    @Test
    void escapesNullAsEmpty() {
        // when
        assertEquals("", ErrorResponses.escape(null));
    }

    /** 普通文本原样输出。 */
    @Test
    void keepsPlainText() {
        // when
        assertEquals("签名校验失败", ErrorResponses.escape("签名校验失败"));
    }

    /** 引号与反斜杠被转义。 */
    @Test
    void escapesQuoteAndBackslash() {
        // when
        assertEquals("a\\\"b\\\\c", ErrorResponses.escape("a\"b\\c"));
    }

    /** 控制字符使用短转义。 */
    @Test
    void escapesControlCharacters() {
        // when
        assertEquals("\\b\\f\\n\\r\\t",
                ErrorResponses.escape("\b\f\n\r\t"));
    }

    /** 其它控制字符使用 unicode 转义。 */
    @Test
    void escapesOtherControlCharacters() {
        // when
        assertEquals("\\u0001", ErrorResponses.escape("\u0001"));
    }

    /** 写出 401 与固定字段。 */
    @Test
    void writesUnauthorizedBody() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        ErrorResponses.writeUnauthorized(response, "a\"b", "line\nbreak");

        // then
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("{\"code\":\"a\\\"b\",\"message\":\"line\\nbreak\"}",
                response.getContentAsString());
    }
}
