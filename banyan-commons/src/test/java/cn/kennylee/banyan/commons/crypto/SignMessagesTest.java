package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名错误码的中英文消息完整性测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignMessagesTest {

    /** 恢复默认语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 每个 SIGN_ 错误码在中英文下都能解析。 */
    @Test
    void allSignCodesResolveInBothLocales() {
        List<ErrorCode> signCodes = Arrays.stream(ErrorCode.values())
                .filter(code -> code.name().startsWith("SIGN_"))
                .toList();
        assertFalse(signCodes.isEmpty(), "缺少 SIGN_ 错误码");

        for (ErrorCode code : signCodes) {
            Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
            String chinese = Messages.get(code, "a");
            assertTrue(chinese != null && !chinese.isBlank(), "中文消息缺失: " + code);
            assertNotEquals(code.messageKey(), chinese, "中文消息缺失: " + code);

            Messages.setLocale(Locale.ENGLISH);
            String english = Messages.get(code, "a");
            assertTrue(english != null && !english.isBlank(), "英文消息缺失: " + code);
            assertNotEquals(code.messageKey(), english, "英文消息缺失: " + code);
        }
    }
}
