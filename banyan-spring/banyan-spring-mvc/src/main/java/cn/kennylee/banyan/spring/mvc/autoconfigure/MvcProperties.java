package cn.kennylee.banyan.spring.mvc.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

        /** Long 序列化为字符串，防 JS 精度丢失。 */
        private boolean longAsString = true;

        /** 枚举按 name 序列化。 */
        private boolean enumsUsingName = true;

        /** BigDecimal 不使用科学计数法。 */
        private boolean bigDecimalAsPlain = true;

        /** {@code java.util.Date} 与 {@code LocalDateTime} 的输入/输出格式；空表示不处理日期。 */
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";

        /** 日期序列化时区；空表示沿用应用默认。 */
        private String timeZone = "";
    }
}
