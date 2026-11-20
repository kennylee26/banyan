package cn.kennylee.banyan.log.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志值工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LogValuesTest {

    /** 手机号脱敏。 */
    @Test
    void masksMobile() {
        // when
        assertThat(LogValues.mask("13812348000")).isEqualTo("138****8000");
    }

    /** 邮箱脱敏。 */
    @Test
    void masksEmail() {
        // when
        assertThat(LogValues.mask("alice@example.com")).isEqualTo("a***@example.com");
    }

    /** 单字符用户名也按规则脱敏。 */
    @Test
    void masksSingleCharacterLocalPart() {
        // when
        assertThat(LogValues.mask("a@b.co")).isEqualTo("a***@b.co");
    }

    /** 空域名不脱敏。 */
    @Test
    void leavesEmptyDomainUntouched() {
        // when
        assertThat(LogValues.mask("ab@")).isEqualTo("ab@");
    }

    /** 其它文本原样返回。 */
    @Test
    void leavesOtherTextUntouched() {
        // when
        assertThat(LogValues.mask("plain-text")).isEqualTo("plain-text");
    }

    /** 超长文本截断。 */
    @Test
    void truncatesLongText() {
        // given
        String value = "x".repeat(300);

        // when
        String truncated = LogValues.truncate(value);

        // then
        assertThat(truncated).hasSizeLessThan(value.length()).endsWith("...");
    }

    /** 短文本原样返回。 */
    @Test
    void keepsShortText() {
        // when
        assertThat(LogValues.truncate("short", 10)).isEqualTo("short");
    }

    /** 恰好等于上限时不截断。 */
    @Test
    void keepsTextAtExactLimit() {
        // when
        assertThat(LogValues.truncate("abc", 3)).isEqualTo("abc");
    }

    /** 上限为 0 时只保留后缀。 */
    @Test
    void truncateAtZeroKeepsOnlySuffix() {
        // when
        assertThat(LogValues.truncate("abc", 0)).isEqualTo("...");
    }

    /** 截断结果精确等于上限加后缀。 */
    @Test
    void truncatesToExactLimit() {
        // when
        assertThat(LogValues.truncate("x".repeat(300), 256))
                .isEqualTo("x".repeat(256) + "...");
    }

    /** null 安全。 */
    @Test
    void handlesNull() {
        // when
        assertThat(LogValues.mask(null)).isNull();
        assertThat(LogValues.truncate(null)).isNull();
    }

    /** 纯静态工具：final、无公开构造器。 */
    @Test
    void isPureStaticUtility() {
        // when
        assertThat(Modifier.isFinal(LogValues.class.getModifiers())).isTrue();
        for (Constructor<?> constructor : LogValues.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
        assertThat(LogValues.class.getAnnotations()).isEmpty();
    }
}
