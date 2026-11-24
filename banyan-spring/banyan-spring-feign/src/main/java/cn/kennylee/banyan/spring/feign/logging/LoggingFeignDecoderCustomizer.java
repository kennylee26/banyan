package cn.kennylee.banyan.spring.feign.logging;

import cn.kennylee.banyan.commons.spring.log.LoggingOptions;
import feign.Capability;
import feign.codec.Decoder;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;

/**
 * 把响应日志解码器挂到每个 Feign Builder 上。
 *
 * <p>{@code FeignBuilderCustomizer} 负责把 {@link Capability} 加进 Builder；
 * 真正的包装由 Feign 官方 SPI 在 enrich 阶段完成，不覆盖使用方声明的 Decoder bean，
 * 也不覆盖其它 FeignBuilderCustomizer。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class LoggingFeignDecoderCustomizer implements FeignBuilderCustomizer {

    /** 日志配置。 */
    private final LoggingOptions logging;

    /**
     * 构造自定义器。
     *
     * @param logging 日志配置
     */
    public LoggingFeignDecoderCustomizer(LoggingOptions logging) {
        this.logging = logging;
    }

    @Override
    public void customize(feign.Feign.Builder builder) {
        builder.addCapability(new LoggingFeignDecoderCapability(logging));
    }
}
