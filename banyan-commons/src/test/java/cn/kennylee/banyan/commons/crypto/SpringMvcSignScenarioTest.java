package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基于 Spring MVC / Servlet 的出站签名与入站验签场景测试。
 *
 * <p>commons 主依赖仍为零 Spring、零第三方；这里只使用 test scope 的
 * spring-boot-starter-test 提供的 {@link MockHttpServletRequest}，验证「使用方
 * 如何把自己的 HTTP 层接到纯 JDK 签名工具上」。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
class SpringMvcSignScenarioTest {

    /** 固定时钟，保证签名与验签时间一致。 */
    private static final Clock CLOCK =
            Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);

    /** 调用方用 SignedRequest.headers() 构造 Servlet 请求，提供方验签通过。 */
    @Test
    void signsAndVerifiesThroughServletHeaders() {
        // given
        SignedRequest signed = signedRequest();
        MockHttpServletRequest request = toServlet(signed);
        ReplayGuard guard = newGuard();

        // when
        SignRequest inbound = fromServlet(request, signed.request().body());
        VerifyResult result = guard.verify(inbound, keys());

        // then
        assertEquals(VerifyResult.SUCCESS, result);
    }

    /** 同一请求重放时被拒绝。 */
    @Test
    void rejectsReplayedServletRequest() {
        // given
        SignedRequest signed = signedRequest();
        MockHttpServletRequest request = toServlet(signed);
        ReplayGuard guard = newGuard();
        SignRequest inbound = fromServlet(request, signed.request().body());

        // when
        assertEquals(VerifyResult.SUCCESS, guard.verify(inbound, keys()));
        assertEquals(VerifyResult.INVALID, guard.verify(inbound, keys()));
    }

    /** 构造待发送结果。 */
    private SignedRequest signedRequest() {
        UnsignedRequest request = UnsignedRequest.of(
                "POST", "/openapi/orders", Map.of("page", List.of("1")), "{}");
        return new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK)
                .sign(request, new SignKey("app1", "secret"));
    }

    /** 构造验签器。 */
    private ReplayGuard newGuard() {
        return new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK, ReplayGuard.DEFAULT_WINDOW_MILLIS);
    }

    /** 构造密钥表。 */
    private Map<String, SignKey> keys() {
        return Map.of("app1", new SignKey("app1", "secret"));
    }

    /** 把 SignedRequest 转成 Servlet 请求：业务数据 + 固定 header。 */
    private MockHttpServletRequest toServlet(SignedRequest signed) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                signed.request().method(), signed.request().path());
        signed.request().queryParams().forEach((name, values) ->
                values.forEach(value -> request.addParameter(name, value)));
        signed.headers().forEach(request::addHeader);
        request.setContent(signed.request().body().getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /** 使用方 Filter 侧从 Servlet 请求还原 SignRequest。 */
    private SignRequest fromServlet(MockHttpServletRequest request, String body) {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) ->
                queryParams.put(name, List.of(values)));
        return new SignRequest(
                request.getMethod(),
                request.getRequestURI(),
                queryParams,
                body,
                Signs.parseTimestamp(request.getHeader(Signs.HEADER_TIMESTAMP)),
                request.getHeader(Signs.HEADER_NONCE),
                request.getHeader(Signs.HEADER_KEY_ID),
                request.getHeader(Signs.HEADER_ALG),
                request.getHeader(Signs.HEADER_SIGN));
    }
}
