package cn.kennylee.banyan.openapi.sign.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.kennylee.banyan.commons.crypto.AesGcmPayloadCipher;
import cn.kennylee.banyan.commons.crypto.HmacSha256SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.InMemoryNonceStore;
import cn.kennylee.banyan.commons.crypto.OpenApiSigner;
import cn.kennylee.banyan.commons.crypto.PayloadCipher;
import cn.kennylee.banyan.commons.crypto.ReplayGuard;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.SignKey;
import cn.kennylee.banyan.commons.crypto.Signs;
import cn.kennylee.banyan.commons.crypto.UnsignedRequest;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.openapi.sign.annotation.OpenApiSigned;
import cn.kennylee.banyan.openapi.sign.autoconfigure.OpenApiSignProperties;
import cn.kennylee.banyan.openapi.sign.support.ConfiguredSignKeyResolver;
import cn.kennylee.banyan.openapi.sign.support.SignKeyResolver;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验签解密拦截器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class OpenApiSignInterceptorTest {

    /** 密钥明文。 */
    private static final String SECRET = "secret";

    /** 注解方法路径。 */
    private static final String PATH = "/openapi/orders";

    /** 明文请求体。 */
    private static final String PLAIN_BODY = "{\"id\":1}";

    /** 密钥解析返回的 keyId 与请求不同时，仍按请求 keyId 查找并使用返回的密钥。 */
    @Test
    void acceptsKeyWhoseIdDiffersFromRequest() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        SignKeyResolver resolver = keyId -> new SignKey("resolved-" + keyId, SECRET);
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(), new InMemoryNonceStore(),
                Clock.systemUTC(), ReplayGuard.DEFAULT_WINDOW_MILLIS, properties.toSignHeaders());
        OpenApiSignInterceptor interceptor = new OpenApiSignInterceptor(properties, guard, resolver,
                properties.toSignHeaders(), new AesGcmPayloadCipher());
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app1", true);

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
    }

    /** 拦截器按自身 header 配置解析，不依赖被替换的 ReplayGuard 内部配置。 */
    @Test
    void doesNotDependOnGuardHeaderConfig() throws Exception {
        // given
        SignHeaders custom = new SignHeaders("X-Acme-Alg", "X-Acme-Key", "X-Acme-Time",
                "X-Acme-Nonce", "X-Acme-Sign");
        OpenApiSignProperties properties = new OpenApiSignProperties();
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(), new InMemoryNonceStore(),
                Clock.systemUTC(), ReplayGuard.DEFAULT_WINDOW_MILLIS, SignHeaders.DEFAULT);
        OpenApiSignInterceptor interceptor = new OpenApiSignInterceptor(properties, guard,
                new ConfiguredSignKeyResolver(Map.of("app1", SECRET)), custom,
                new AesGcmPayloadCipher());
        OpenApiSigner.SignedPayload payload = OpenApiSigner.builder()
                .keyId("app1").secret(SECRET).headers(custom).build()
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));
        CachedBodyRequestWrapper request = wrap(payload, PATH);

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
    }

    /** 未标注方法直接放行。 */
    @Test
    void passesUnannotatedHandler() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "open"));

        // then
        assertTrue(allowed);
        assertEquals(200, response.getStatus());
    }

    /** 合法请求验签并解密，明文写入请求属性。 */
    @Test
    void verifiesAndReplacesBody() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app1", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
        Object plaintext = request.getAttribute(CachedBodyRequestWrapper.PLAIN_BODY_ATTRIBUTE);
        assertNotNull(plaintext);
        assertEquals(PLAIN_BODY, new String((byte[]) plaintext, StandardCharsets.UTF_8));
    }

    /** 类级注解同样生效。 */
    @Test
    void verifiesClassLevelAnnotation() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app1", true);

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new ClassLevelController(), "handle"));

        // then
        assertTrue(allowed);
    }

    /** 签名被篡改时返回 401 与通用错误体。 */
    @Test
    void rejectsTamperedSignature() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = tamperedRequest(PATH, PLAIN_BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"openapi.sign.invalid\""));
        assertTrue(body.contains(Messages.get(ErrorCode.OPENAPI_SIGN_INVALID)));
        assertFalse(body.contains("SIGNATURE_MISMATCH"));
    }

    /** 开启具体原因后返回对应文案且 code 不变。 */
    @Test
    void revealsReasonWhenEnabled() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.setRevealReason(true);
        OpenApiSignInterceptor interceptor = interceptor(properties);
        CachedBodyRequestWrapper request = expiredRequest(PATH, PLAIN_BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"openapi.sign.invalid\""));
        assertTrue(body.contains(Messages.get(ErrorCode.OPENAPI_SIGN_EXPIRED)));
    }

    /** 未知 keyId 被拒绝。 */
    @Test
    void rejectsUnknownKeyId() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app2", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    /** 验签通过但解密失败时仍返回 401。 */
    @Test
    void rejectsUndecryptableBody() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = plainSignedRequest(PATH, "not-base64!!", "app1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    /** 关闭加密后接受明文签名，不替换 body。 */
    @Test
    void acceptsPlaintextWhenEncryptionDisabled() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.setEncrypted(false);
        OpenApiSignInterceptor interceptor = interceptor(properties);
        CachedBodyRequestWrapper request = plainSignedRequest(PATH, PLAIN_BODY, "app1");

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
        assertNull(request.getAttribute(CachedBodyRequestWrapper.PLAIN_BODY_ATTRIBUTE));
    }

    /** 空 body 只验签、不解密。 */
    @Test
    void acceptsEmptyBody() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = signedRequest(PATH, null, "app1", true);

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
    }

    /** 失败日志包含具体原因与定位信息，且不含敏感内容。 */
    @Test
    void logsSpecificReasonWithoutSecrets() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = tamperedRequest(PATH, PLAIN_BODY);
        String signature = request.getHeader(Signs.HEADER_SIGN);
        Logger logger = (Logger) LoggerFactory.getLogger(OpenApiSignInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when
        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));
        logger.detachAppender(appender);

        // then
        assertFalse(appender.list.isEmpty());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("SIGNATURE_MISMATCH"));
        assertTrue(message.contains("POST"));
        assertTrue(message.contains(PATH));
        assertTrue(message.contains("app1"));
        assertFalse(message.contains(SECRET));
        assertFalse(message.contains(signature));
        assertFalse(message.contains(PLAIN_BODY));
    }

    /** 未被过滤器包装时拒绝，不按空 body 放行。 */
    @Test
    void rejectsMissingCachedBody() throws Exception {
        // given
        OpenApiSigner.SignedPayload payload = OpenApiSigner.builder()
                .keyId("app1").secret(SECRET).build()
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), null));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
        payload.headers().forEach(request::addHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor(new OpenApiSignProperties()).preHandle(request,
                response, handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    /** 带 context-path 时按去掉前缀的 path 验签。 */
    @Test
    void stripsContextPathBeforeVerify() throws Exception {
        // given
        OpenApiSignInterceptor interceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper request = signedRequestWithContextPath(PLAIN_BODY);

        // when
        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // then
        assertTrue(allowed);
    }

    /** 默认配置下不同失败原因返回完全相同的错误体。 */
    @Test
    void returnsSameDefaultBodyForEveryFailure() throws Exception {
        // given
        OpenApiSignInterceptor replayInterceptor = interceptor(new OpenApiSignProperties());
        CachedBodyRequestWrapper replayRequest = signedRequest(PATH, PLAIN_BODY, "app1", true);
        replayInterceptor.preHandle(replayRequest, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));

        // when
        String tampered = rejectionBody(tamperedRequest(PATH, PLAIN_BODY));
        String expired = rejectionBody(expiredRequest(PATH, PLAIN_BODY));
        String unknown = rejectionBody(signedRequest(PATH, PLAIN_BODY, "app2", true));
        String replay = rejectionBody(replayInterceptor, replayRequest);
        String decrypt = rejectionBody(plainSignedRequest(PATH, "not-base64!!", "app1"));

        // then
        assertEquals(tampered, expired);
        assertEquals(tampered, unknown);
        assertEquals(tampered, replay);
        assertEquals(tampered, decrypt);
    }

    /** 自定义加密实现抛任意运行时异常时仍返回 401。 */
    @Test
    void rejectsWhenCipherThrowsRuntimeException() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        PayloadCipher failing = new PayloadCipher() {

            @Override
            public String name() {
                return "failing";
            }

            @Override
            public String encrypt(String plaintext, String secret) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String secret) {
                throw new IllegalStateException("boom");
            }
        };
        OpenApiSignInterceptor interceptor = new OpenApiSignInterceptor(properties,
                new ReplayGuard(new HmacSha256SignAlgorithm(), new InMemoryNonceStore(),
                        Clock.systemUTC(), ReplayGuard.DEFAULT_WINDOW_MILLIS,
                        properties.toSignHeaders()),
                new ConfiguredSignKeyResolver(Map.of("app1", SECRET)),
                properties.toSignHeaders(), failing);
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app1", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    /** 开启具体原因时，每种失败都返回对应文案。 */
    @Test
    void revealsEachFailureReason() throws Exception {
        // when
        assertRevealed(tamperedRequest(PATH, PLAIN_BODY),
                ErrorCode.OPENAPI_SIGN_SIGNATURE_MISMATCH);
        assertRevealed(expiredRequest(PATH, PLAIN_BODY), ErrorCode.OPENAPI_SIGN_EXPIRED);
        assertRevealed(malformedRequest(), ErrorCode.OPENAPI_SIGN_MALFORMED);
        assertRevealed(algorithmMismatchRequest(), ErrorCode.OPENAPI_SIGN_ALG_MISMATCH);
        assertRevealed(signedRequest(PATH, PLAIN_BODY, "app2", true),
                ErrorCode.OPENAPI_SIGN_UNKNOWN_KEY);
        assertRevealed(plainSignedRequest(PATH, "not-base64!!", "app1"),
                ErrorCode.PAYLOAD_DECRYPT_FAILED);
        assertReplayRevealed();
    }

    /** 断言失败响应包含指定原因文案。 */
    private void assertRevealed(CachedBodyRequestWrapper request, ErrorCode expected)
            throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.setRevealReason(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor(properties).preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains(Messages.get(expected)));
    }

    /** 同一请求第二次到达时返回重放文案。 */
    private void assertReplayRevealed() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.setRevealReason(true);
        OpenApiSignInterceptor interceptor = interceptor(properties);
        CachedBodyRequestWrapper request = signedRequest(PATH, PLAIN_BODY, "app1", true);

        // when
        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new TestController(), "signed"));
        MockHttpServletResponse second = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, second,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertTrue(second.getContentAsString()
                .contains(Messages.get(ErrorCode.OPENAPI_SIGN_REPLAY)));
    }

    /** 断言请求被拒绝并返回响应体。 */
    private String rejectionBody(CachedBodyRequestWrapper request) throws Exception {
        return rejectionBody(interceptor(new OpenApiSignProperties()), request);
    }

    /** 用指定拦截器断言请求被拒绝并返回响应体。 */
    private String rejectionBody(OpenApiSignInterceptor interceptor,
                                 CachedBodyRequestWrapper request) throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean allowed = interceptor.preHandle(request, response,
                handler(new TestController(), "signed"));

        // then
        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        return response.getContentAsString();
    }

    /** 构造带 context-path 的签名请求。 */
    private CachedBodyRequestWrapper signedRequestWithContextPath(String plainBody) throws Exception {
        // given
        OpenApiSigner.SignedPayload payload = OpenApiSigner.builder()
                .keyId("app1").secret(SECRET).build()
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), plainBody));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + PATH);
        request.setContextPath("/api");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(payload.body().getBytes(StandardCharsets.UTF_8));
        payload.headers().forEach(request::addHeader);

        // when
        return new CachedBodyRequestWrapper(request);
    }

    /** 构造缺少签名字段的请求。 */
    private CachedBodyRequestWrapper malformedRequest() throws Exception {
        return new CachedBodyRequestWrapper(new MockHttpServletRequest("POST", PATH));
    }

    /** 构造算法标识不匹配的请求。 */
    private CachedBodyRequestWrapper algorithmMismatchRequest() throws Exception {
        OpenApiSigner.SignedPayload payload = OpenApiSigner.builder()
                .keyId("app1").secret(SECRET).build()
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(payload.body().getBytes(StandardCharsets.UTF_8));
        payload.headers().forEach((name, value) -> request.addHeader(name,
                name.equals(Signs.HEADER_ALG) ? "OTHER" : value));
        return new CachedBodyRequestWrapper(request);
    }

    /** 构造拦截器。 */
    private OpenApiSignInterceptor interceptor(OpenApiSignProperties properties) {
        SignKeyResolver resolver = new ConfiguredSignKeyResolver(Map.of("app1", SECRET));
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(), Clock.systemUTC(),
                ReplayGuard.DEFAULT_WINDOW_MILLIS, properties.toSignHeaders());
        return new OpenApiSignInterceptor(properties, guard, resolver,
                properties.toSignHeaders(), new AesGcmPayloadCipher());
    }

    /** 构造加密签名请求。 */
    private CachedBodyRequestWrapper signedRequest(String path, String plainBody,
                                                   String keyId, boolean encrypt) throws Exception {
        OpenApiSigner.Builder builder = OpenApiSigner.builder()
                .keyId(keyId)
                .secret(keyId.equals("app1") ? SECRET : "secret2");
        if (!encrypt) {
            builder.plainBody();
        }
        OpenApiSigner.SignedPayload payload = builder.build().sign(
                UnsignedRequest.of("POST", path, Map.of(), plainBody));
        return wrap(payload, path);
    }

    /** 构造只签名、不加密的请求。 */
    private CachedBodyRequestWrapper plainSignedRequest(String path, String body, String keyId) throws Exception {
        return signedRequest(path, body, keyId, false);
    }

    /** 构造签名被篡改的请求。 */
    private CachedBodyRequestWrapper tamperedRequest(String path, String plainBody) throws Exception {
        OpenApiSigner.SignedPayload payload = OpenApiSigner.builder()
                .keyId("app1").secret(SECRET).build()
                .sign(UnsignedRequest.of("POST", path, Map.of(), plainBody));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(payload.body().getBytes(StandardCharsets.UTF_8));
        payload.headers().forEach((name, value) -> request.addHeader(name,
                name.equals(Signs.HEADER_SIGN) ? value + "0" : value));
        return new CachedBodyRequestWrapper(request);
    }

    /** 构造过期请求。 */
    private CachedBodyRequestWrapper expiredRequest(String path, String plainBody) throws Exception {
        SignKey key = new SignKey("app1", SECRET);
        String encrypted = new AesGcmPayloadCipher().encrypt(plainBody, SECRET);
        Map<String, String> params = Signs.sign(
                UnsignedRequest.of("POST", path, Map.of(), encrypted), key,
                new HmacSha256SignAlgorithm(),
                Clock.fixed(Instant.ofEpochMilli(1L), ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(encrypted.getBytes(StandardCharsets.UTF_8));
        request.addHeader(Signs.HEADER_ALG, params.get(Signs.PARAM_ALG));
        request.addHeader(Signs.HEADER_KEY_ID, params.get(Signs.PARAM_KEY_ID));
        request.addHeader(Signs.HEADER_TIMESTAMP, params.get(Signs.PARAM_TIMESTAMP));
        request.addHeader(Signs.HEADER_NONCE, params.get(Signs.PARAM_NONCE));
        request.addHeader(Signs.HEADER_SIGN, params.get(Signs.PARAM_SIGN));
        return new CachedBodyRequestWrapper(request);
    }

    /** 把签名结果写入可重复读请求。 */
    private CachedBodyRequestWrapper wrap(OpenApiSigner.SignedPayload payload, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = payload.body() == null ? "" : payload.body();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        payload.headers().forEach(request::addHeader);
        return new CachedBodyRequestWrapper(request);
    }

    /** 构造 HandlerMethod。 */
    private HandlerMethod handler(Object controller, String method) {
        try {
            return new HandlerMethod(controller, controller.getClass().getMethod(method, String.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 测试用 Controller。 */
    static class TestController {

        /** 需要验签的方法。 */
        @OpenApiSigned
        public void signed(String body) {
        }

        /** 未标注的方法。 */
        public void open(String body) {
        }
    }

    /** 类级注解 Controller。 */
    @OpenApiSigned
    static class ClassLevelController {

        /** 类级注解覆盖的方法。 */
        public void handle(String body) {
        }
    }
}
