package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import com.baomidou.mybatisplus.annotation.DbType;

/**
 * 可控制方言判定的测试子类：记录探测入参并按预设结果返回。
 *
 * @author kennylee
 * @since 0.1.0
 */
class DialectControlledUserDas extends UserDas {

    private final boolean allowMultiValueInsert;

    int probes;

    DbType probedDbType;

    String probedJdbcUrl;

    DialectControlledUserDas(UserMapper userMapper, DasQueryStrategyRegistry registry,
                             boolean allowMultiValueInsert) {
        super(userMapper, registry);
        this.allowMultiValueInsert = allowMultiValueInsert;
    }

    @Override
    protected boolean supportsMultiValueInsert(DbType dbType, String jdbcUrl) {
        probes++;
        probedDbType = dbType;
        probedJdbcUrl = jdbcUrl;
        return allowMultiValueInsert;
    }
}
