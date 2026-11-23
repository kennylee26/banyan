package cn.kennylee.banyan.spring.feign.logging;

import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 透明响应日志解码器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LoggingFeignDecoderTest {

    /** 打印响应后仍能让原 Decoder 读到完整 body。 */
    @Test
    void delegatesWithReadableBody() throws Exception {
        // given
        Decoder delegate = (response, type) -> new String(
                response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LoggingFeignDecoder decoder = new LoggingFeignDecoder(delegate,
                new FeignProperties().getLogging());
        Response response = response("{\"id\":1}");

        // when
        Object result = decoder.decode(response, String.class);

        // then
        assertEquals("{\"id\":1}", result);
    }

    /** 原 Decoder 抛异常时原样传播。 */
    @Test
    void propagatesDelegateFailure() {
        // given
        IllegalStateException failure = new IllegalStateException("decode failed");
        Decoder delegate = (response, type) -> {
            throw failure;
        };
        LoggingFeignDecoder decoder = new LoggingFeignDecoder(delegate,
                new FeignProperties().getLogging());

        // when / then
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> decoder.decode(response("bad"), String.class)));
    }

    /** 构造 Feign Response。 */
    private static Response response(String body) {
        Request request = Request.create(Request.HttpMethod.GET, "/hello",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(Collections.emptyMap())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}
