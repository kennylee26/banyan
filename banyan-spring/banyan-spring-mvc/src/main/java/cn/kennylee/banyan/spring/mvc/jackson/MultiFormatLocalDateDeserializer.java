package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 按多个候选格式解析 {@code LocalDate}，全部失败时交给 Jackson 报错。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MultiFormatLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private final List<DateTimeFormatter> formatters;

    /**
     * 构造实例。
     *
     * @param formatters 候选格式，按顺序尝试
     */
    public MultiFormatLocalDateDeserializer(List<DateTimeFormatter> formatters) {
        this.formatters = List.copyOf(formatters);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (StringUtils.hasText(text)) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(text, formatter);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一个候选格式
                }
            }
        }
        return (LocalDate) context.handleWeirdStringValue(LocalDate.class, text,
                Messages.get(ErrorCode.SPRING_MVC_DATE_PARSE_FAILED, text));
    }
}
