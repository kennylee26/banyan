package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 宽松布尔解析：接受布尔值、数字 {@code 0/1}、字符串 {@code "true"/"false"/"0"/"1"}。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class LenientBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (token == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            int value = parser.getIntValue();
            if (value == 0) {
                return Boolean.FALSE;
            }
            if (value == 1) {
                return Boolean.TRUE;
            }
        }
        if (token == JsonToken.VALUE_STRING) {
            String text = parser.getText().trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return Boolean.FALSE;
            }
        }
        return (Boolean) context.reportInputMismatch(Boolean.class,
                Messages.get(ErrorCode.SPRING_MVC_BOOLEAN_PARSE_FAILED, parser.getValueAsString()));
    }
}
