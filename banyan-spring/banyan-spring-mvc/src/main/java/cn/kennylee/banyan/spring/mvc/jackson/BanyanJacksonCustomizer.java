package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.spring.mvc.autoconfigure.MvcProperties;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 可选 Jackson 增强：按开关应用 Long 转字符串、枚举按 name、BigDecimal 非科学计数法。
 *
 * <p>不替换容器 {@code ObjectMapper}，也不声明 {@code HttpMessageConverters} bean；
 * 排序晚于 Spring Boot 自身 customizer，使本模块开关优先。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class BanyanJacksonCustomizer implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

    private final MvcProperties.Jackson options;

    /**
     * 构造实例。
     *
     * @param options Jackson 增强选项
     */
    public BanyanJacksonCustomizer(MvcProperties.Jackson options) {
        this.options = options;
    }

    @Override
    public void customize(Jackson2ObjectMapperBuilder builder) {
        if (options.isLongAsString()) {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        }
        if (options.isEnumsUsingName()) {
            builder.featuresToDisable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        } else {
            builder.featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        }
        if (options.isBigDecimalAsPlain()) {
            builder.featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        } else {
            builder.featuresToDisable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
