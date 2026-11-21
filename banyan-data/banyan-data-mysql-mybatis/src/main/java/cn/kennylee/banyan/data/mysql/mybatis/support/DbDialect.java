package cn.kennylee.banyan.data.mysql.mybatis.support;

import com.baomidou.mybatisplus.annotation.DbType;

/**
 * 数据库方言能力判断。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class DbDialect {

    private DbDialect() {
    }

    /**
     * 判断方言是否支持多值 INSERT（{@code INSERT INTO t (...) VALUES (...),(...)}）。
     * <p>支持时批量保存可用单条 SQL 多行获得更高吞吐；Oracle 等不支持多值 VALUES 的方言需回退到
     * JDBC 批量执行器。</p>
     *
     * @param dbType 数据库类型
     * @return 是否支持多值 INSERT
     */
    public static boolean supportsMultiValueInsert(DbType dbType) {
        return dbType == DbType.MYSQL
                || dbType == DbType.MARIADB
                || dbType == DbType.H2;
    }
}
