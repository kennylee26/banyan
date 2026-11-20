package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 调用方签名器测试：一次调用得到原始请求和可附加到 HTTP 请求上的签名参数。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RequestSignerTest {

    /** 原始请求与签名参数一起返回，使用方不必重新拼装内部协议对象。 */
    @Test
    void signsCompleteRequestForThirdPartyCall() {
        // given
        UnsignedRequest request = UnsignedRequest.of(
                "POST", "/openapi/orders", Map.of("page", List.of("1")), "{}");
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);
        RequestSigner signer = new RequestSigner(new HmacSha256SignAlgorithm(), clock);

        // when
        SignedRequest signed = signer.sign(request, new SignKey("app1", "secret"));

        // then
        assertEquals(request, signed.request());
        assertEquals("1700000000000", signed.authParams().get(Signs.PARAM_TIMESTAMP));
        assertEquals("app1", signed.authParams().get(Signs.PARAM_KEY_ID));

        SignRequest inbound = new SignRequest(
                signed.request().method(), signed.request().path(),
                signed.request().queryParams(), signed.request().body(),
                Signs.parseTimestamp(signed.authParams().get(Signs.PARAM_TIMESTAMP)),
                signed.authParams().get(Signs.PARAM_NONCE),
                signed.authParams().get(Signs.PARAM_KEY_ID),
                signed.authParams().get(Signs.PARAM_ALG),
                signed.authParams().get(Signs.PARAM_SIGN));
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(clock), clock, ReplayGuard.DEFAULT_WINDOW_MILLIS);

        assertEquals(VerifyResult.SUCCESS,
                guard.verify(inbound, Map.of("app1", new SignKey("app1", "secret"))));
    }

    /** 返回的签名参数不可被修改。 */
    @Test
    void returnedAuthParamsAreImmutable() {
        // given
        RequestSigner signer = new RequestSigner();
        SignedRequest signed = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                new SignKey("app1", "secret"));

        // when
        assertThrows(UnsupportedOperationException.class,
                () -> signed.authParams().put(Signs.PARAM_SIGN, "changed"));
        assertNotNull(signed.authParams().get(Signs.PARAM_SIGN));
    }

    /** 构造后修改外层 map 或内层 list 不影响已创建请求。 */
    @Test
    void requestIsImmutableAfterConstruction() {
        // given
        java.util.List<String> values = new java.util.ArrayList<>(java.util.List.of("1"));
        java.util.Map<String, java.util.List<String>> params =
                new java.util.HashMap<>();
        params.put("page", values);
        UnsignedRequest request = UnsignedRequest.of("GET", "/openapi/ping", params, null);

        // when
        params.put("other", java.util.List.of("2"));
        values.add("2");

        // then
        assertEquals(1, request.queryParams().size());
        assertEquals(java.util.List.of("1"), request.queryParams().get("page"));
    }

    /** 通过访问器修改外层 map 或内层 list 都会失败。 */
    @Test
    void accessorsReturnImmutableViews() {
        // given
        UnsignedRequest request = UnsignedRequest.of("GET", "/openapi/ping",
                java.util.Map.of("page", java.util.List.of("1")), null);

        // when
        assertThrows(UnsupportedOperationException.class,
                () -> request.queryParams().put("other", java.util.List.of("2")));
        assertThrows(UnsupportedOperationException.class,
                () -> request.queryParams().get("page").add("2"));
    }

    /** 请求被修改后不应让既有签名失配。 */
    @Test
    void signedRequestStaysConsistentWhenCallerMutatesInput() {
        // given
        java.util.List<String> values = new java.util.ArrayList<>(java.util.List.of("1"));
        java.util.Map<String, java.util.List<String>> params = new java.util.HashMap<>();
        params.put("page", values);
        UnsignedRequest request = UnsignedRequest.of("GET", "/openapi/ping", params, null);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);
        SignedRequest signed = new RequestSigner(new HmacSha256SignAlgorithm(), clock)
                .sign(request, new SignKey("app1", "secret"));

        // when
        values.add("2");
        params.put("other", java.util.List.of("3"));

        // then
        assertEquals(java.util.List.of("1"), signed.request().queryParams().get("page"));
        assertNotNull(signed.authParams().get(Signs.PARAM_SIGN));
    }
}
