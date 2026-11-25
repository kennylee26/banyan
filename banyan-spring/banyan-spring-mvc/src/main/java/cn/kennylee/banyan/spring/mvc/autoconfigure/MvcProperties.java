package cn.kennylee.banyan.spring.mvc.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring MVC 能力的 Jackson 增强配置。总开关与各 advice 开关由 {@code @ConditionalOnProperty} 直接读取，不在此类声明。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.spring.mvc")
public class MvcProperties {

    /** Jackson 增强配置。 */
    private Jackson jackson = new Jackson();

    /**
     * Jackson 增强配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Jackson {

        /** 默认 Date / LocalDateTime 格式。 */
        public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

        /** 默认 LocalDate 格式。 */
        public static final String DEFAULT_LOCAL_DATE_FORMAT = "yyyy-MM-dd";

        /** Long 序列化为字符串，防 JS 精度丢失。 */
        private boolean longAsString = true;

        /** 枚举按 name 序列化。 */
        private boolean enumsUsingName = true;

        /** BigDecimal 不使用科学计数法。 */
        private boolean bigDecimalAsPlain = true;

        /** 宽松布尔解析：接受布尔值、数字 0/1、字符串 "true"/"false"/"0"/"1"。 */
        private boolean lenientBoolean = true;

        /** {@code java.util.Date} 的输出格式，也是 {@code LocalDateTime} 反序列化的首选格式；空表示不处理日期。 */
        private String dateFormat = DEFAULT_DATE_FORMAT;

        /** {@code LocalDateTime} 反序列化的额外候选格式，按顺序尝试；序列化仍用 {@link #dateFormat}。 */
        private List<String> dateFormats = new ArrayList<>();

        /** {@code LocalDate} 的输出格式，也是其反序列化的首选格式；不能带时间字段。 */
        private String localDateFormat = DEFAULT_LOCAL_DATE_FORMAT;

        /** {@code LocalDate} 反序列化的额外候选格式，按顺序尝试；序列化仍用 {@link #localDateFormat}。 */
        private List<String> localDateFormats = new ArrayList<>();

        /** 日期序列化时区；空表示沿用应用默认。 */
        private String timeZone = "";
    }
}
