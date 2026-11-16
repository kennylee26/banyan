package cn.kennylee.banyan.data.mongodb;

import cn.kennylee.banyan.commons.error.BanyanException;

import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mongodb.strategy.MongodbDasQueryStrategy;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MongoDB DAS 端到端测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@DataMongoTest
@Import(MongoDasTest.TestConfig.class)
class MongoDasTest {

    @Autowired
    private UserDas userDas;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection(UserEo.class);
    }

    /** 保存后可按主键查询。 */
    @Test
    void savesAndFindsById() {
        UserEo user = newUser("alice", 30);

        userDas.save(user);

        assertThat(user.getId()).isNotNull();
        assertThat(userDas.findById(user.getId()).getUserName()).isEqualTo("alice");
    }

    /** 按主键删除。 */
    @Test
    void removesById() {
        UserEo user = newUser("alice", 30);
        userDas.save(user);

        assertThat(userDas.removeById(user.getId())).isTrue();
        assertThat(userDas.findById(user.getId())).isNull();
    }

    /** 批量保存中途失败时回滚已插入记录。 */
    @Test
    void saveBatchRollsBackOnFailure() {
        UserEo first = newUser("peter", 90);
        first.setId(new ObjectId());
        UserEo duplicate = newUser("peter2", 91);
        duplicate.setId(first.getId());

        assertThatThrownBy(() -> userDas.saveBatch(List.of(first, duplicate)))
                .isInstanceOf(BanyanException.class);
        // 失败后整批不应残留
        assertThat(userDas.count()).isZero();
    }

    /** 忽略错误的批量保存：重复主键不影响其余记录。 */
    @Test
    void saveBatchIgnoreErrorsKeepsGoing() {
        UserEo first = newUser("olive", 80);
        first.setId(new ObjectId());
        UserEo duplicate = newUser("olive2", 81);
        duplicate.setId(first.getId());

        List<UserEo> result = userDas.saveBatchIgnoreErrors(List.of(first, duplicate));

        assertThat(result).hasSize(2);
        // 重复主键的那条被忽略，只写入 1 条
        assertThat(userDas.count()).isEqualTo(1);
    }

    /** 批量保存。 */
    @Test
    void savesBatch() {
        List<UserEo> users = List.of(newUser("alice", 30), newUser("bob", 20));

        userDas.saveBatch(users);

        assertThat(users).allMatch(user -> user.getId() != null);
        assertThat(userDas.count()).isEqualTo(2);
    }

    /** 动态条件分页。 */
    @Test
    void findPageWithDynamicConditions() {
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        UserPageQuery query = new UserPageQuery();
        query.setUserNameLike("ali");
        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getUserName()).isEqualTo("alice");
    }

    /** 无排序时保持稳定顺序。 */
    @Test
    void findPageKeepsStableOrderWithoutSorts() {
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        PageResult<UserEo> first = userDas.findPage(new UserPageQuery());
        PageResult<UserEo> second = userDas.findPage(new UserPageQuery());

        assertThat(first.data()).extracting(UserEo::getId)
                .containsExactlyElementsOf(second.data().stream().map(UserEo::getId).toList());
    }

    /** 流式查询全部。 */
    @Test
    void streamsAll() {
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        assertThat(userDas.findAllStream().count()).isEqualTo(2);
    }

    private UserEo newUser(String userName, int age) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(age);
        return user;
    }

    static class UserPageQuery extends PageDto {
        private String userNameLike;

        public String getUserNameLike() {
            return userNameLike;
        }

        public void setUserNameLike(String userNameLike) {
            this.userNameLike = userNameLike;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        DasQueryStrategyRegistry dasQueryStrategyRegistry() {
            return new DefaultDasQueryStrategyRegistry(
                    Map.of(DasDbType.MONGODB, new MongodbDasQueryStrategy<>(UserEo.class)));
        }

        @Bean
        UserDas userDas(MongoTemplate mongoTemplate, DasQueryStrategyRegistry registry) {
            return new UserDas(mongoTemplate, registry);
        }
    }
}
