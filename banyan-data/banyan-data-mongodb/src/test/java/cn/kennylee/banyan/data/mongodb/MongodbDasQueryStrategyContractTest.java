package cn.kennylee.banyan.data.mongodb;

import cn.kennylee.banyan.data.mongodb.strategy.MongodbDasQueryStrategy;
import cn.kennylee.banyan.test.contract.DasQueryStrategyContract;

import java.util.List;
import java.util.Map;

/**
 * MongoDB 查询策略的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MongodbDasQueryStrategyContractTest extends DasQueryStrategyContract {

    @Override
    protected Object parse(Map<String, Object> query) {
        return new MongodbDasQueryStrategy<UserEo>().parseQuery(query, UserEo.class, List.of());
    }
}
