package cn.kennylee.banyan.openapi.sign.web;

import cn.kennylee.banyan.commons.crypto.OpenApiSigner;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.Signs;
import cn.kennylee.banyan.commons.crypto.UnsignedRequest;
import cn.kennylee.banyan.openapi.sign.annotation.OpenApiSigned;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring 容器级端到端测试：自动配置注册的过滤器与拦截器接入 MockMvc，
 * 完整走加密请求 → 验签 → 解密 → Controller 明文。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = OpenApiSignSpringMvcEndToEndTest.TestApplication.class,
        properties = "banyan.openapi-sign.keys.app1=secret")
@AutoConfigureMockMvc
class OpenApiSignSpringMvcEndToEndTest {

    /** 方法级注解端点。 */
    private static final String METHOD_PATH = "/openapi/orders";

    /** 类级注解端点。 */
    private static final String CLASS_PATH = "/openapi/reports";

    /** 未标注端点。 */
    private static final String OPEN_PATH = "/openapi/health";

    /** 明文请求体。 */
    private static final String PLAIN_BODY = "{\"id\":1}";

    /** 由容器装配的 MockMvc。 */
    @Autowired
    private MockMvc mvc;

    /** 加密请求经容器装配的过滤器与拦截器解密后，Controller 拿到明文。 */
    @Test
    void decryptsThroughSpringMvc() throws Exception {
        // given
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", METHOD_PATH, Map.of(), PLAIN_BODY));

        // when
        mvc.perform(post(METHOD_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(payload.body())
                        .headers(SignTestSupport.headers(payload)))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 类级注解同样生效，Controller 方法无需重复标注。 */
    @Test
    void appliesClassLevelAnnotation() throws Exception {
        // given
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", CLASS_PATH, Map.of(), PLAIN_BODY));

        // when
        mvc.perform(post(CLASS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(payload.body())
                        .headers(SignTestSupport.headers(payload)))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 篡改签名后由容器级拦截器返回 401。 */
    @Test
    void rejectsTamperedSignature() throws Exception {
        // given
        OpenApiSigner.SignedPayload payload = SignTestSupport.signer(true, SignHeaders.DEFAULT)
                .sign(UnsignedRequest.of("POST", METHOD_PATH, Map.of(), PLAIN_BODY));
        var request = post(METHOD_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .content(payload.body());
        payload.headers().forEach((name, value) -> request.header(name,
                name.equals(Signs.HEADER_SIGN) ? value + "0" : value));

        // when
        mvc.perform(request)
                // then
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("openapi.sign.invalid")));
    }

    /** 未标注注解的端点不经过验签，空 body 也放行。 */
    @Test
    void letsUnannotatedEndpointThrough() throws Exception {
        // when
        mvc.perform(post(OPEN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PLAIN_BODY))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(PLAIN_BODY));
    }

    /** 测试用启动配置：只加载自动配置并注册 Controller，不做组件扫描。 */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({OrderController.class, ReportController.class, HealthController.class})
    static class TestApplication {
    }

    /** 方法级注解端点。 */
    @RestController
    static class OrderController {

        /**
         * 接收并原样返回解密后的请求体。
         *
         * @param body 请求体
         * @return 请求体
         */
        @PostMapping(METHOD_PATH)
        @OpenApiSigned
        public String create(@RequestBody String body) {
            return body;
        }
    }

    /** 类级注解端点。 */
    @RestController
    @OpenApiSigned
    static class ReportController {

        /**
         * 接收并原样返回解密后的请求体。
         *
         * @param body 请求体
         * @return 请求体
         */
        @PostMapping(CLASS_PATH)
        public String create(@RequestBody String body) {
            return body;
        }
    }

    /** 未标注端点。 */
    @RestController
    static class HealthController {

        /**
         * 原样返回请求体。
         *
         * @param body 请求体
         * @return 请求体
         */
        @PostMapping(OPEN_PATH)
        public String echo(@RequestBody String body) {
            return body;
        }
    }
}
