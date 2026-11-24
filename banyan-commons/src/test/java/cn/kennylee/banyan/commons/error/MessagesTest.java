package cn.kennylee.banyan.commons.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 消息解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MessagesTest {

    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 中文环境下解析中文消息并填充占位符。 */
    @Test
    void resolvesChinese() {
        // given
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);

        // when
        assertEquals("查询字段在实体上不存在: name", Messages.get(ErrorCode.UNKNOWN_PROPERTY, "name"));
    }

    /** 英文环境下解析英文消息。 */
    @Test
    void resolvesEnglish() {
        // given
        Messages.setLocale(Locale.ENGLISH);

        // when
        assertEquals("Query property does not exist on entity: name",
                Messages.get(ErrorCode.UNKNOWN_PROPERTY, "name"));
    }

    /** JVM 默认语言为英文时，显式切中文仍读中文，不回退到英文 bundle。 */
    @Test
    void resolvesChineseWhenJvmDefaultIsEnglish() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            Messages.setLocale(Locale.SIMPLIFIED_CHINESE);

            assertEquals("未找到表信息: users",
                    Messages.get(ErrorCode.TABLE_INFO_NOT_FOUND, "users"));
        } finally {
            Locale.setDefault(original);
            Messages.setLocale(original);
        }
    }

    /** 资源缺失时返回 key 本身，便于排查。 */
    @Test
    void missingKeyFallsBackToKey() {
        // given
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver(Locale.ENGLISH);

        // when
        assertEquals("banyan.unknown.key", resolver.resolve("banyan.unknown.key"));
    }
}
