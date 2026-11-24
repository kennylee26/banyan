package cn.kennylee.banyan.spring.feign.logging;

import cn.kennylee.banyan.commons.log.LogSanitizer;
import cn.kennylee.banyan.commons.log.LoggingOptions;
import feign.Response;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 透明响应日志解码器：打印响应后用缓存字节重建 Response，再委托原 Decoder。
 *
 * <p>不判断业务返回体是否成功，不改变原解码结果或异常；body 为空时不重建。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class LoggingFeignDecoder implements Decoder {

    /** 被包装的解码器。 */
    private final Decoder delegate;

    /** 日志配置。 */
    private final LoggingOptions logging;

    /**
     * 构造包装解码器。
     *
     * @param delegate 原解码器
     * @param logging  日志配置
     */
    public LoggingFeignDecoder(Decoder delegate, LoggingOptions logging) {
        this.delegate = delegate;
        this.logging = logging;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.body() == null) {
            log(response, "");
            return delegate.decode(response, type);
        }
        byte[] bytes;
        try (java.io.InputStream body = response.body().asInputStream()) {
            bytes = body.readAllBytes();
        }
        String text = LogSanitizer.body(LogSanitizer.utf8(bytes), logging);
        log(response, text);
        Response replay = response.toBuilder().body(bytes).build();
        return delegate.decode(replay, type);
    }

    /** 打印响应状态与脱敏后的 header/body。 */
    private void log(Response response, String body) {
        if (!log.isDebugEnabled()) {
            return;
        }
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        response.headers().forEach((name, values) ->
                headers.put(name, values.isEmpty() ? "" : values.iterator().next()));
        log.debug("Feign 响应: status={}, headers={}, body={}", response.status(),
                LogSanitizer.headers(headers, logging), body);
    }
}
