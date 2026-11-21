package cn.kennylee.banyan.data.mysql.mybatis.support;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库方言能力判断测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class DbDialectTest {

    /** MySQL 家族与 H2 支持多值 INSERT。 */
    @Test
    void supportsMysqlFamilyAndH2() {
        // when
        assertThat(DbDialect.supportsMultiValueInsert(DbType.MYSQL)).isTrue();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.MARIADB)).isTrue();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.H2)).isTrue();
    }

    /** 真实 JDBC URL 能解析出 H2；{@code DbType.getDbType(String)} 对同一 URL 返回 OTHER，不能用。 */
    @Test
    void resolvesDialectFromJdbcUrl() {
        // when
        assertThat(JdbcUtils.getDbType("jdbc:h2:mem:banyan;DB_CLOSE_DELAY=-1")).isEqualTo(DbType.H2);
        assertThat(JdbcUtils.getDbType("jdbc:mysql://localhost:3306/demo")).isEqualTo(DbType.MYSQL);
    }

    /** Oracle 等方言不支持多值 INSERT。 */
    @Test
    void rejectsDialectsWithoutMultiValueInsert() {
        // when
        assertThat(DbDialect.supportsMultiValueInsert(DbType.ORACLE)).isFalse();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.POSTGRE_SQL)).isFalse();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.SQL_SERVER)).isFalse();
    }
}
