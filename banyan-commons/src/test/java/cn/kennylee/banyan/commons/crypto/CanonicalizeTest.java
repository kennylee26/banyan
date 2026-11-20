package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规范化规则测试：golden vector、字段敏感性、签名参数排除、编码与排序。
 *
 * @author kennylee
 * @since 0.1.0
 */
class CanonicalizeTest {

    /** 固定请求体。 */
    private static final String BODY = "{\"id\":1}";

    /** 固定请求体摘要。 */
    private static final String BODY_HASH =
            "037c9214eef74cc3887f3a4f085b4e17d76280dafd273b0ee160c09c4ba1cfd4";

    /** 标准请求。 */
    private SignRequest request() {
        return new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "HmacSHA256", "sig");
    }

    /** golden vector 固定。 */
    @Test
    void producesGoldenVector() {
        // given
        String expected = String.join("\n",
                "BANYAN-SIGN-V1", "POST", "/openapi/orders", "a=x+y&b=1&b=2", BODY_HASH,
                "1700000000000", "abc", "app1", "HmacSHA256");

        // when
        assertEquals(expected, Signs.canonicalize(request()));
    }

    /** 任一参与字段变化都会改变规范化串。 */
    @Test
    void anyFieldChangeAltersCanonical() {
        // given
        String base = Signs.canonicalize(request());

        // when
        assertNotEquals(base, Signs.canonicalize(new SignRequest("PUT", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/other",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x z"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                "{\"id\":2}", 1700000000000L, "abc", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000001L, "abc", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abd", "app1", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app2", "HmacSHA256", "sig")));
        assertNotEquals(base, Signs.canonicalize(new SignRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "TEST", "sig")));
    }

    /** sign 自身不参与规范化。 */
    @Test
    void excludesSignatureField() {
        // when
        assertFalse(Signs.canonicalize(request()).contains("sign="));
    }

    /** 五个协议参数无论放 header 还是 query 都不参与规范化。 */
    @Test
    void excludesAllProtocolParams() {
        // given
        SignRequest request = new SignRequest("GET", "/openapi/ping",
                Map.of("alg", List.of("OTHER"), "keyId", List.of("other"),
                        "timestamp", List.of("1"), "nonce", List.of("n"),
                        "sign", List.of("s"), "a", List.of("1")),
                null, 1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // when
        String canonical = Signs.canonicalize(request);

        // then
        assertTrue(canonical.contains("a=1"));
        assertFalse(canonical.contains("alg=OTHER"));
        assertFalse(canonical.contains("keyId=other"));
        assertFalse(canonical.contains("timestamp=1"));
        assertFalse(canonical.contains("nonce=n"));
        assertFalse(canonical.contains("sign=s"));
    }

    /** 空值跳过。 */
    @Test
    void skipsBlankValues() {
        // given
        SignRequest request = new SignRequest("GET", "/openapi/ping",
                Map.of("empty", List.of(""), "blank", List.of("  "), "ok", List.of("1")),
                null, 1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // when
        String canonical = Signs.canonicalize(request);

        // then
        assertFalse(canonical.contains("empty="));
        assertFalse(canonical.contains("blank="));
        assertTrue(canonical.contains("ok=1"));
    }

    /** 重复参数按值字典序。 */
    @Test
    void sortsDuplicateValues() {
        // given
        SignRequest request = new SignRequest("GET", "/openapi/ping",
                Map.of("b", List.of("2", "1")), null,
                1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // when
        assertTrue(Signs.canonicalize(request).contains("b=1&b=2"));
    }

    /** 值只做一次 UTF-8 URL 编码。 */
    @Test
    void encodesExactlyOnce() {
        // given
        SignRequest request = new SignRequest("GET", "/openapi/ping",
                Map.of("q", List.of("a b"), "p", List.of("100%")), null,
                1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // when
        String canonical = Signs.canonicalize(request);

        // then
        assertTrue(canonical.contains("q=a+b"));
        assertTrue(canonical.contains("p=100%25"));
        assertFalse(canonical.contains("%2525"));
        assertFalse(canonical.contains("a%2Bb"));
    }

    /** 未签名的出站请求不能参与规范化。 */
    @Test
    void rejectsUnsignedRequest() {
        // given
        SignRequest unsigned = new SignRequest("GET", "/openapi/ping", Map.of(), null,
                0L, null, null, null, null);

        // when
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Signs.canonicalize(unsigned));
    }

    /** 时间戳边界：0 与负数拒绝，1 接受。 */
    @Test
    void enforcesTimestampBoundaries() {
        // when
        assertThrows(IllegalArgumentException.class, () -> Signs.canonicalize(
                new SignRequest("GET", "/openapi/ping", Map.of(), null,
                        0L, "abc", "app1", "HmacSHA256", "sig")));
        assertThrows(IllegalArgumentException.class, () -> Signs.canonicalize(
                new SignRequest("GET", "/openapi/ping", Map.of(), null,
                        -1L, "abc", "app1", "HmacSHA256", "sig")));

        String canonical = Signs.canonicalize(new SignRequest("GET", "/openapi/ping",
                Map.of(), null, 1L, "abc", "app1", "HmacSHA256", "sig"));

        // then
        assertTrue(canonical.contains("\n1\n"));
    }

    /** null 查询参数按空 Map 处理。 */
    @Test
    void acceptsNullQueryParams() {
        // given
        SignRequest request = new SignRequest("GET", "/openapi/ping", null, null,
                1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // when
        assertTrue(request.queryParams().isEmpty());
    }

    /** 非空 key 的 null 值列表被过滤。 */
    @Test
    void filtersNullValueLists() {
        // given
        Map<String, List<String>> params = new HashMap<>();
        params.put("b", null);

        // when
        SignRequest request = new SignRequest("GET", "/openapi/ping", params, null,
                1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // then
        assertFalse(request.queryParams().containsKey("b"));
    }

    /** 空 key 与 null 值在构造时被过滤。 */
    @Test
    void filtersBlankKeysAndNullValues() {
        // given
        Map<String, List<String>> params = new HashMap<>();
        params.put("", List.of("x"));
        params.put("a", Arrays.asList("1", null));

        // when
        SignRequest request = new SignRequest("GET", "/openapi/ping", params, null,
                1700000000000L, "abc", "app1", "HmacSHA256", "sig");

        // then
        assertFalse(request.queryParams().containsKey(""));
        assertEquals(1, request.queryParams().get("a").size());
        assertFalse(Signs.canonicalize(request).contains("=x"));
    }
}
