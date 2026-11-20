package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SignedRequest 的 HTTP header 传输约定测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignedRequestHeadersTest {

    /** 五个协议参数都有固定 header 名。 */
    @Test
    void exposesFiveFixedHeaders() {
        // given
        RequestSigner signer = new RequestSigner();
        SignedRequest signed = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                new SignKey("app1", "secret"));

        // when
        Map<String, String> headers = signed.headers();

        // then
        assertEquals(5, headers.size());
        assertEquals(signed.authParams().get(Signs.PARAM_ALG),
                headers.get(Signs.HEADER_ALG));
        assertEquals(signed.authParams().get(Signs.PARAM_KEY_ID),
                headers.get(Signs.HEADER_KEY_ID));
        assertEquals(signed.authParams().get(Signs.PARAM_TIMESTAMP),
                headers.get(Signs.HEADER_TIMESTAMP));
        assertEquals(signed.authParams().get(Signs.PARAM_NONCE),
                headers.get(Signs.HEADER_NONCE));
        assertEquals(signed.authParams().get(Signs.PARAM_SIGN),
                headers.get(Signs.HEADER_SIGN));
    }

    /** headers 视图不可被修改。 */
    @Test
    void headersAreImmutable() {
        // given
        SignedRequest signed = new RequestSigner().sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                new SignKey("app1", "secret"));

        // when
        assertThrows(UnsupportedOperationException.class,
                () -> signed.headers().put(Signs.HEADER_SIGN, "changed"));
    }
}
