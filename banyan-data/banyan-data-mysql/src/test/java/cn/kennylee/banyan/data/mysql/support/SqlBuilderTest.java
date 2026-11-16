package cn.kennylee.banyan.data.mysql.support;

import cn.kennylee.banyan.commons.error.BanyanException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SQL 文本构造测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SqlBuilderTest {

    /** 多值 INSERT 的占位符按行重复。 */
    @Test
    void buildsMultiValueInsert() {
        String sql = SqlBuilder.insertSql("users", List.of("id", "user_name"), 2);

        assertThat(sql).isEqualTo("INSERT INTO users (id,user_name) VALUES (?,?),(?,?)");
    }

    /** 列或行数非法时快速失败。 */
    @Test
    void rejectsIllegalArguments() {
        assertThatThrownBy(() -> SqlBuilder.insertSql("users", List.of(), 1))
                .isInstanceOf(BanyanException.class);
        assertThatThrownBy(() -> SqlBuilder.insertSql("users", List.of("id"), 0))
                .isInstanceOf(BanyanException.class);
    }
}
