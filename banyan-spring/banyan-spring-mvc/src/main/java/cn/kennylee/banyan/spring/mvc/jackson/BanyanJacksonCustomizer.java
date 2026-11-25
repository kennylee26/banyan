package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.spring.mvc.autoconfigure.MvcProperties;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 增强：Long 转字符串、枚举按 name、BigDecimal 非科学计数法，以及 Date / LocalDateTime 的统一日期格式。
 *
 * <p>不替换容器 {@code ObjectMapper}，也不声明 {@code HttpMessageConverters} bean；
 * 排序晚于 Spring Boot 自身 customizer，使本模块开关优先。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class BanyanJacksonCustomizer implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

    private static final String JSR310_MODULE = "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule";

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
        applyDateTime(builder);
    }

    /**
     * 统一 Date 与 LocalDateTime 的输入/输出格式。{@code date-format} 为空时不动日期。
     *
     * @param builder Jackson builder
     */
    private void applyDateTime(Jackson2ObjectMapperBuilder builder) {
        if (!StringUtils.hasText(options.getDateFormat())) {
            return;
        }
        builder.simpleDateFormat(options.getDateFormat());
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        if (ClassUtils.isPresent(JSR310_MODULE, BanyanJacksonCustomizer.class.getClassLoader())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(options.getDateFormat());
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        }
        if (StringUtils.hasText(options.getTimeZone())) {
            builder.timeZone(options.getTimeZone());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
