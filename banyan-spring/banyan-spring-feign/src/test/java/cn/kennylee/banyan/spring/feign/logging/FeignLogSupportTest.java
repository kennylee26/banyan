package cn.kennylee.banyan.spring.feign.logging;

import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import org.junit.jupiter.api.Test;

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
class FeignLogSupportTest {

    /** 敏感头默认脱敏，忽略头完全不打印。 */
    @Test
    void masksSensitiveHeadersAndSkipsIgnoredHeaders() {
        // given
        FeignProperties properties = new FeignProperties();
        properties.getLogging().setIgnoredHeaders(List.of("X-Internal"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("Cookie", "sid=1");
        headers.put("X-Tenant", "t1");
        headers.put("X-Internal", "hidden");

        // when
        String text = FeignLogSupport.headers(headers, properties.getLogging());

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
        FeignProperties properties = new FeignProperties();
        properties.getLogging().setMaskSensitive(false);

        // when
        String text = FeignLogSupport.headers(Map.of("Authorization", "Bearer secret"),
                properties.getLogging());

        // then
        assertTrue(text.contains("Authorization=Bearer secret"));
    }

    /** body 超长时截断并标记。 */
    @Test
    void truncatesLongBody() {
        // given
        FeignProperties properties = new FeignProperties();
        properties.getLogging().setMaxBodyLength(8);

        // when
        String text = FeignLogSupport.body("0123456789", properties.getLogging());

        // then
        assertEquals("01234567...<truncated>", text);
    }
}
