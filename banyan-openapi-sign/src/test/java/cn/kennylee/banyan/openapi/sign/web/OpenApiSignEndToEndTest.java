package cn.kennylee.banyan.openapi.sign.web;

import cn.kennylee.banyan.commons.crypto.AesGcmPayloadCipher;
import cn.kennylee.banyan.commons.crypto.HmacSha256SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.InMemoryNonceStore;
import cn.kennylee.banyan.commons.crypto.OpenApiSigner;
import cn.kennylee.banyan.commons.crypto.ReplayGuard;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.Signs;
import cn.kennylee.banyan.commons.crypto.UnsignedRequest;
import cn.kennylee.banyan.openapi.sign.annotation.OpenApiSigned;
import cn.kennylee.banyan.openapi.sign.autoconfigure.OpenApiSignProperties;
import cn.kennylee.banyan.openapi.sign.support.ConfiguredSignKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注解接入的端到端测试：过滤器 + 拦截器 + Controller。
 *
 * @author kennylee
 * @since 0.1.0
 */
class OpenApiSignEndToEndTest {

    /** 路径。 */
    private static final String PATH = "/openapi/orders";

    /** 明文请求体。 */
    private static final String PLAIN_BODY = "{\"id\":1}";

    /** 加密请求进入 Controller 时拿到明文。 */
    @Test
    void deliversPlaintextToController() throws Exception {
        // given
        MockMvc mvc = mvc(new OpenApiSignProperties());
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));

        // when
        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(payload.body())
                        .headers(SignTestSupport.headers(payload)))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 篡改密文返回 401，Controller 不执行。 */
    @Test
    void rejectsTamperedPayload() throws Exception {
        // given
        MockMvc mvc = mvc(new OpenApiSignProperties());
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));
        var request = post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(payload.body());
        payload.headers().forEach((name, value) -> request.header(name,
                name.equals(Signs.HEADER_SIGN) ? value + "0" : value));

        // when
        mvc.perform(request)
                // then
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("openapi.sign.invalid")));
    }

    /** 自定义 header 名端到端生效。 */
    @Test
    void supportsCustomHeaderNames() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.getHeaders().setAlg("X-Acme-Alg");
        properties.getHeaders().setKeyId("X-Acme-Key");
        properties.getHeaders().setTimestamp("X-Acme-Time");
        properties.getHeaders().setNonce("X-Acme-Nonce");
        properties.getHeaders().setSign("X-Acme-Sign");
        MockMvc mvc = mvc(properties);
        SignHeaders headersConfig = properties.toSignHeaders();
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, headersConfig)
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));

        // when
        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.body())
                        .headers(SignTestSupport.headers(payload)))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 关闭加密后按明文验签。 */
    @Test
    void acceptsPlaintextWhenEncryptionDisabled() throws Exception {
        // given
        OpenApiSignProperties properties = new OpenApiSignProperties();
        properties.setEncrypted(false);
        MockMvc mvc = mvc(properties);
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(false, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", PATH, Map.of(), PLAIN_BODY));

        // when
        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.body())
                        .headers(SignTestSupport.headers(payload)))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 构造 MockMvc。 */
    private MockMvc mvc(OpenApiSignProperties properties) {
        OpenApiSignInterceptor interceptor = new OpenApiSignInterceptor(properties,
                new ReplayGuard(new HmacSha256SignAlgorithm(), new InMemoryNonceStore(),
                        Clock.systemUTC(), ReplayGuard.DEFAULT_WINDOW_MILLIS,
                        properties.toSignHeaders()),
                new ConfiguredSignKeyResolver(Map.of("app1", "secret")),
                properties.toSignHeaders(), new AesGcmPayloadCipher());
        return MockMvcBuilders.standaloneSetup(new OrderController())
                .addFilters(new RepeatableBodyFilter())
                .addInterceptors(interceptor)
                .build();
    }

    /** 测试用 Controller。 */
    @RestController
    static class OrderController {

        /**
         * 接收并原样返回解密后的请求体。
         *
         * @param body 请求体
         * @return 请求体
         */
        @PostMapping(PATH)
        @OpenApiSigned
        public String create(@RequestBody String body) {
            return body;
        }
    }
}
