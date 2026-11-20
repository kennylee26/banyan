package cn.kennylee.banyan.commons.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 统一异常测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class BanyanExceptionTest {

    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 异常携带错误码与参数，消息按当前语言解析。 */
    @Test
    void carriesErrorCodeAndArgs() {
        // given
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);

        // when
        BanyanException exception = new BanyanException(ErrorCode.TABLE_INFO_NOT_FOUND, "users");

        // then
        assertEquals(ErrorCode.TABLE_INFO_NOT_FOUND, exception.getErrorCode());
        assertEquals("未找到表信息: users", exception.getMessage());
    }

    /** 原始异常保留在 cause 中。 */
    @Test
    void keepsCause() {
        // given
        RuntimeException cause = new RuntimeException("boom");

        // when
        BanyanException exception = new BanyanException(ErrorCode.BATCH_SAVE_FAILED, cause);

        // then
        assertSame(cause, exception.getCause());
    }
}
