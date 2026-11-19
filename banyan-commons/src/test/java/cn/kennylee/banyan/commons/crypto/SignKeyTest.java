package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 签名密钥值对象测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignKeyTest {

    /** 固定中文消息。 */
    @BeforeEach
    void fixLocale() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    /** 恢复语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** keyId 与 secret 不能为空。 */
    @Test
    void rejectsBlankFields() {
        assertThrows(IllegalArgumentException.class, () -> new SignKey("", "secret"));
        assertThrows(IllegalArgumentException.class, () -> new SignKey("  ", "secret"));
        assertThrows(IllegalArgumentException.class, () -> new SignKey("app1", ""));
        assertThrows(IllegalArgumentException.class, () -> new SignKey("app1", null));
    }

    /** 异常消息走错误码，不泄漏 keyId 与 secret。 */
    @Test
    void invalidKeyMessageDoesNotLeakSecret() {
        IllegalArgumentException blankKeyId = assertThrows(IllegalArgumentException.class,
                () -> new SignKey("", "top-secret-value"));
        IllegalArgumentException blankSecret = assertThrows(IllegalArgumentException.class,
                () -> new SignKey("app1", ""));

        assertFalse(blankKeyId.getMessage().contains("top-secret-value"));
        assertFalse(blankSecret.getMessage().contains("app1"));
    }

    /** toString 不输出 secret。 */
    @Test
    void toStringRedactsSecret() {
        SignKey key = new SignKey("app1", "top-secret-value");

        assertFalse(key.toString().contains("top-secret-value"));
        assertEquals("app1", key.keyId());
        assertEquals("top-secret-value", key.secret());
    }
}
