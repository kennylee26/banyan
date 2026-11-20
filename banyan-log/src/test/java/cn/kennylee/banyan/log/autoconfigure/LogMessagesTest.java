package cn.kennylee.banyan.log.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志消息资源完整性测试：每个 LOG_ 错误码在中英文下都必须能解析。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LogMessagesTest {

    /** 恢复默认语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 新增的日志错误码在中英文下都有消息。 */
    @Test
    void allLogCodesResolveInBothLocales() {
        // given
        List<ErrorCode> logCodes = Arrays.stream(ErrorCode.values())
                .filter(code -> code.name().startsWith("LOG_"))
                .toList();

        // when
        assertThat(logCodes).isNotEmpty();

        for (ErrorCode code : logCodes) {
            Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
            String chinese = Messages.get(code, "a", "b", "c", "d");
            assertThat(chinese).as("中文消息缺失: %s", code).isNotBlank()
                    .isNotEqualTo(code.messageKey());

            Messages.setLocale(Locale.ENGLISH);
            String english = Messages.get(code, "a", "b", "c", "d");
            assertThat(english).as("英文消息缺失: %s", code).isNotBlank()
                    .isNotEqualTo(code.messageKey());
        }
    }
}
