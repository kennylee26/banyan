package cn.kennylee.banyan.commons.log;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日志脱敏与截断测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LogSanitizerTest {

    /** 敏感头默认脱敏，忽略头完全不打印。 */
    @Test
    void masksSensitiveHeadersAndSkipsIgnoredHeaders() {
        // given
        LoggingOptions options = new LoggingOptions();
        options.setIgnoredHeaders(List.of("X-Internal"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("Cookie", "sid=1");
        headers.put("X-Tenant", "t1");
        headers.put("X-Internal", "hidden");

        // when
        String text = LogSanitizer.headers(headers, options);

        // then
        assertTrue(text.contains("Authorization=***"));
        assertTrue(text.contains("Cookie=***"));
        assertTrue(text.contains("X-Tenant=t1"));
        assertFalse(text.contains("X-Internal"));
        assertFalse(text.contains("Bearer secret"));
    }

    /** 关闭脱敏后打印原值。 */
    @Test
    void printsValuesWhenMaskingDisabled() {
        // given
        LoggingOptions options = new LoggingOptions();
        options.setMaskSensitive(false);

        // when
        String text = LogSanitizer.headers(Map.of("Authorization", "Bearer secret"), options);

        // then
        assertTrue(text.contains("Authorization=Bearer secret"));
    }

    /** body 超长时截断并标记。 */
    @Test
    void truncatesLongBody() {
        // given
        LoggingOptions options = new LoggingOptions();
        options.setMaxBodyLength(8);

        // when
        String text = LogSanitizer.body("0123456789", options);

        // then
        assertEquals("01234567...<truncated>", text);
    }

    /** 空值与空 body 都按空串处理。 */
    @Test
    void treatsNullAsEmpty() {
        // given
        LoggingOptions options = new LoggingOptions();

        // when
        String body = LogSanitizer.body(null, options);

        // then
        assertEquals("", body);
        assertEquals("", LogSanitizer.utf8(null));
        assertEquals("abc", LogSanitizer.utf8("abc".getBytes(StandardCharsets.UTF_8)));
    }
}
