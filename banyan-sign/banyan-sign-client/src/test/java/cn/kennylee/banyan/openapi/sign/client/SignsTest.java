package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议内核规范化测试：与服务端共用同一组 golden vector。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignsTest {

    /** 固定请求体。 */
    private static final String BODY = "{\"id\":1}";

    /** 固定请求体摘要，与服务端 golden vector 一致。 */
    private static final String BODY_HASH =
            "037c9214eef74cc3887f3a4f085b4e17d76280dafd273b0ee160c09c4ba1cfd4";

    /** 规范化串 golden vector。 */
    @Test
    void producesGoldenVector() {
        // given
        CanonicalRequest request = new CanonicalRequest("POST", "/openapi/orders",
                Map.of("b", List.of("2", "1"), "a", List.of("x y"), "sign", List.of("ignored")),
                BODY, 1700000000000L, "abc", "app1", "Hmac" + "SHA256");
        String expected = String.join("\n",
                "BANYAN-SIGN-V1", "POST", "/openapi/orders", "a=x+y&b=1&b=2", BODY_HASH,
                "1700000000000", "abc", "app1", "Hmac" + "SHA256");

        // when
        assertEquals(expected, Signs.canonicalize(request));
    }

    /** 五个协议参数不参与规范化。 */
    @Test
    void excludesProtocolParams() {
        // given
        CanonicalRequest request = new CanonicalRequest("GET", "/openapi/ping",
                Map.of("alg", List.of("OTHER"), "keyId", List.of("other"),
                        "timestamp", List.of("1"), "nonce", List.of("n"),
                        "sign", List.of("s"), "a", List.of("1")),
                null, 1700000000000L, "abc", "app1", "Hmac" + "SHA256");

        // when
        String canonical = Signs.canonicalize(request);

        // then
        assertTrue(canonical.contains("a=1"));
        assertFalse(canonical.contains("alg=OTHER"));
        assertFalse(canonical.contains("keyId=other"));
    }

    /** 空值跳过，同名参数按字典序。 */
    @Test
    void skipsBlankValuesAndSorts() {
        // given
        CanonicalRequest request = new CanonicalRequest("GET", "/openapi/ping",
                Map.of("empty", List.of(""), "blank", List.of("  "),
                        "b", List.of("2", "1")),
                null, 1700000000000L, "abc", "app1", "Hmac" + "SHA256");

        // when
        String canonical = Signs.canonicalize(request);

        // then
        assertTrue(canonical.contains("b=1&b=2"));
        assertFalse(canonical.contains("empty="));
        assertFalse(canonical.contains("blank="));
    }

    /** 请求体摘要与服务端一致。 */
    @Test
    void hashesBodyWithSha256() {
        // when
        assertEquals(BODY_HASH, Signs.sha256(BODY));
    }

    /** nonce 为 32 位小写十六进制且每次不同。 */
    @Test
    void generatesRandomNonce() {
        // when
        String first = Signs.newNonce();
        String second = Signs.newNonce();

        // then
        assertEquals(32, first.length());
        assertTrue(first.matches("[0-9a-f]{32}"));
        assertNotEquals(first, second);
    }
}
