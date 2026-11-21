package cn.kennylee.banyan.data.mongodb.springdata;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mongodb.springdata.das.AbstractMongoDas;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * 测试数据访问类。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class UserDas extends AbstractMongoDas<UserEo, ObjectId> {

    public UserDas(MongoTemplate mongoTemplate, DasQueryStrategyRegistry registry) {
        super(mongoTemplate, registry);
    }

    @Override
    public Class<UserEo> getEntityClass() {
        return UserEo.class;
    }
}
