package cn.kennylee.banyan.data.mysql.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LIKE 特殊字符转义测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LikeEscaperTest {

    /** 转义符本身与 SQL 标准通配符。 */
    @Test
    void escapesPercentUnderscoreAndBackslash() {
        assertThat(LikeEscaper.escape("50%")).isEqualTo("50\\%");
        assertThat(LikeEscaper.escape("a_b")).isEqualTo("a\\_b");
        assertThat(LikeEscaper.escape("a\\b")).isEqualTo("a\\\\b");
        assertThat(LikeEscaper.escape("%%__")).isEqualTo("\\%\\%\\_\\_");
    }

    /** 用户输入本身含反斜杠时，反斜杠作为字面量保留。 */
    @Test
    void keepsUserBackslashAsLiteral() {
        assertThat(LikeEscaper.escape("50\\%")).isEqualTo("50\\\\\\%");
        assertThat(LikeEscaper.escape("\\%")).isEqualTo("\\\\\\%");
    }

    /** 空值与 null 原样返回。 */
    @Test
    void returnsEmptyAndNullUnchanged() {
        assertThat(LikeEscaper.escape("")).isEqualTo("");
        assertThat(LikeEscaper.escape(null)).isNull();
    }

    /** 标准转义不是幂等的：重复转义会改变结果，因此两层必须保证只调用一次。 */
    @Test
    void escapingTwiceChangesTheValue() {
        String once = LikeEscaper.escape("50%");
        String twice = LikeEscaper.escape(once);

        assertThat(once).isEqualTo("50\\%");
        assertThat(twice).isEqualTo("50\\\\\\%");
        assertThat(twice).isNotEqualTo(once);
    }

    /** 不含特殊字符的值不改变。 */
    @Test
    void keepsPlainValueUnchanged() {
        assertThat(LikeEscaper.escape("alice")).isEqualTo("alice");
    }
}
