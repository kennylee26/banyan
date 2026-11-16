package cn.kennylee.banyan.data.mysql.support;

import com.baomidou.mybatisplus.annotation.DbType;
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
        assertThat(DbDialect.supportsMultiValueInsert(DbType.MYSQL)).isTrue();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.MARIADB)).isTrue();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.H2)).isTrue();
    }

    /** Oracle 等方言不支持多值 INSERT。 */
    @Test
    void rejectsDialectsWithoutMultiValueInsert() {
        assertThat(DbDialect.supportsMultiValueInsert(DbType.ORACLE)).isFalse();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.POSTGRE_SQL)).isFalse();
        assertThat(DbDialect.supportsMultiValueInsert(DbType.SQL_SERVER)).isFalse();
    }
}
