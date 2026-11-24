package cn.kennylee.banyan.commons.http;

import org.junit.jupiter.api.Test;

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

    /** 默认阻塞连接与鉴权头，大小写无关。 */
    @Test
    void removesDefaultBlockedHeadersCaseInsensitively() {
        // given
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("cookie", "sid=1");
        headers.put("X-Keep", "ok");

        // when
        HeaderRules.removeBlocked(headers, List.of());

        // then
        assertFalse(headers.containsKey("Authorization"));
        assertFalse(headers.containsKey("cookie"));
        assertEquals(Map.of("X-Keep", "ok"), headers);
    }

    /** 额外阻塞头大小写无关。 */
    @Test
    void removesExtraBlockedHeaders() {
        // given
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-tenant", "t1");
        headers.put("X-Keep", "ok");

        // when
        HeaderRules.removeBlocked(headers, List.of("X-TENANT"));

        // then
        assertFalse(headers.containsKey("x-tenant"));
        assertTrue(headers.containsKey("X-Keep"));
    }

    /** 额外阻塞头为 null 时只应用默认规则。 */
    @Test
    void toleratesNullExtraBlocked() {
        // given
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Keep", "ok");

        // when
        HeaderRules.removeBlocked(headers, null);

        // then
        assertEquals(Map.of("X-Keep", "ok"), headers);
    }

    /** 配置与单次上下文的阻塞头一并生效。 */
    @Test
    void mergesOptionsAndContextBlockedHeaders() {
        // given
        HeaderForwardOptions options = new HeaderForwardOptions();
        options.getBlockedHeaders().add("X-Option");
        HeaderContext context = new HeaderContext();
        context.getBlockedHeaders().add("X-Context");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Option", "1");
        headers.put("x-context", "2");
        headers.put("X-Keep", "3");

        // when
        HeaderRules.removeBlocked(headers, options, context);

        // then
        assertEquals(Map.of("X-Keep", "3"), headers);
    }

    /** 配置与上下文为 null 时退化为默认规则。 */
    @Test
    void toleratesNullOptionsAndContext() {
        // given
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("X-Keep", "3");

        // when
        HeaderRules.removeBlocked(headers, (HeaderForwardOptions) null, null);

        // then
        assertEquals(Map.of("X-Keep", "3"), headers);
    }
}
