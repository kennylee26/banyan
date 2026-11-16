package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字符串工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class StringsTest {

    /** 空白判断覆盖 null、空串与空白字符。 */
    @Test
    void blankDetectionCoversNullEmptyAndWhitespace() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank("   "));
        assertFalse(Strings.isBlank("a"));

        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank("  "));
        assertTrue(Strings.isNotBlank("a"));
    }

    /** 驼峰转下划线。 */
    @Test
    void camelCaseToUnderline() {
        assertEquals("user_name", Strings.toUnderlineCase("userName"));
        assertEquals("id", Strings.toUnderlineCase("id"));
        assertEquals("url", Strings.toUnderlineCase("URL"));
        assertEquals("user_id", Strings.toUnderlineCase("userID"));
        assertEquals("a_bc", Strings.toUnderlineCase("aBC"));
    }

    /** 下划线转换对 null 返回 null。 */
    @Test
    void toUnderlineCaseKeepsNullAsNull() {
        assertNull(Strings.toUnderlineCase(null));
    }

    /** 下划线转驼峰。 */
    @Test
    void underlineToCamelCase() {
        assertEquals("userName", Strings.toCamelCase("user_name"));
        assertEquals("name", Strings.toCamelCase("name"));
        assertNull(Strings.toCamelCase(null));
    }
}
