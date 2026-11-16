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
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);

        assertEquals("查询字段在实体上不存在: name", Messages.get(ErrorCode.UNKNOWN_PROPERTY, "name"));
    }

    /** 英文环境下解析英文消息。 */
    @Test
    void resolvesEnglish() {
        Messages.setLocale(Locale.ENGLISH);

        assertEquals("Query property does not exist on entity: name",
                Messages.get(ErrorCode.UNKNOWN_PROPERTY, "name"));
    }

    /** 资源缺失时返回 key 本身，便于排查。 */
    @Test
    void missingKeyFallsBackToKey() {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver(Locale.ENGLISH);

        assertEquals("banyan.unknown.key", resolver.resolve("banyan.unknown.key"));
    }
}
