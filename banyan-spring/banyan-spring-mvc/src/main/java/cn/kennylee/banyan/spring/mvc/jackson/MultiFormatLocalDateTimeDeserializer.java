package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 按多个候选格式解析 {@code LocalDateTime}，全部失败时交给 Jackson 报错。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private final List<DateTimeFormatter> formatters;

    /**
     * 构造实例。
     *
     * @param formatters 候选格式，按顺序尝试
     */
    public MultiFormatLocalDateTimeDeserializer(List<DateTimeFormatter> formatters) {
        this.formatters = List.copyOf(formatters);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (StringUtils.hasText(text)) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(text, formatter);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一个候选格式
                }
            }
        }
        return (LocalDateTime) context.handleWeirdStringValue(LocalDateTime.class, text,
                Messages.get(ErrorCode.SPRING_MVC_DATE_PARSE_FAILED, text));
    }
}
