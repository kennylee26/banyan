package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.mysql.strategy.MyBatisDasQueryStrategy;
import cn.kennylee.banyan.test.contract.DasQueryStrategyContract;

import java.util.List;
import java.util.Map;

/**
 * MySQL 查询策略的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MyBatisDasQueryStrategyContractTest extends DasQueryStrategyContract {

    @Override
    protected Object parse(Map<String, Object> query) {
        return new MyBatisDasQueryStrategy<UserEo>().parseQuery(query, UserEo.class, List.of());
    }
}
