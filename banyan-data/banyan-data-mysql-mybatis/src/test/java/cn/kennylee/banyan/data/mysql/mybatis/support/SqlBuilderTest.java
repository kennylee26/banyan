package cn.kennylee.banyan.data.mysql.mybatis.support;

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
        // when
        String sql = SqlBuilder.insertSql("users", List.of("id", "user_name"), 2);

        // then
        assertThat(sql).isEqualTo("INSERT INTO users (id,user_name) VALUES (?,?),(?,?)");
    }

    /** CASE WHEN 批量更新：每列按有值的行生成分支，末尾 ELSE 列名保留原值，主键只出现在 WHERE。 */
    @Test
    void buildsCaseWhenBatchUpdate() {
        // when
        String sql = SqlBuilder.updateCaseWhenSql("users", "id",
                List.of("user_name", "age"), List.of(2, 1), 2);

        // then
        assertThat(sql).isEqualTo(
                "UPDATE users SET user_name = CASE id WHEN ? THEN ? WHEN ? THEN ? ELSE user_name END, "
                        + "age = CASE id WHEN ? THEN ? ELSE age END WHERE id IN (?,?)");
        assertThat(sql).doesNotContain("id = CASE");
    }

    /** CASE WHEN 更新的列清单与行数非法时快速失败。 */
    @Test
    void rejectsIllegalCaseWhenArguments() {
        // when
        assertThatThrownBy(() -> SqlBuilder.updateCaseWhenSql("users", "id", List.of(), List.of(), 1))
                .isInstanceOf(BanyanException.class);
        assertThatThrownBy(() -> SqlBuilder.updateCaseWhenSql("users", "id", List.of("age"), List.of(1), 0))
                .isInstanceOf(BanyanException.class);
        assertThatThrownBy(() -> SqlBuilder.updateCaseWhenSql("users", "id", List.of("age"), List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 列或行数非法时快速失败。 */
    @Test
    void rejectsIllegalArguments() {
        // when
        assertThatThrownBy(() -> SqlBuilder.insertSql("users", List.of(), 1))
                .isInstanceOf(BanyanException.class);
        assertThatThrownBy(() -> SqlBuilder.insertSql("users", List.of("id"), 0))
                .isInstanceOf(BanyanException.class);
    }
}
