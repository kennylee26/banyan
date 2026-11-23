package cn.kennylee.banyan.spring.feign.logging;

import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import feign.Capability;
import feign.codec.Decoder;

/**
 * 通过 Feign Capability SPI 包装当前 Decoder，打印响应日志。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class LoggingFeignDecoderCapability implements Capability {

    /** 日志配置。 */
    private final FeignProperties.Logging logging;

    /**
     * 构造能力。
     *
     * @param logging 日志配置
     */
    public LoggingFeignDecoderCapability(FeignProperties.Logging logging) {
        this.logging = logging;
    }

    @Override
    public Decoder enrich(Decoder decoder) {
        return decoder instanceof LoggingFeignDecoder
                ? decoder : new LoggingFeignDecoder(decoder, logging);
    }
}
