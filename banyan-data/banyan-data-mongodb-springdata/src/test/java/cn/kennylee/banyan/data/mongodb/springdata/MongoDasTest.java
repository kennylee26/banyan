package cn.kennylee.banyan.data.mongodb.springdata;

import cn.kennylee.banyan.commons.error.BanyanException;

import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mongodb.springdata.das.AbstractMongoDas;
import cn.kennylee.banyan.data.mongodb.springdata.strategy.MongodbDasQueryStrategy;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collection;
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
        // given
        UserEo user = newUser("alice", 30);

        // when
        userDas.save(user);

        // then
        assertThat(user.getId()).isNotNull();
        assertThat(userDas.findById(user.getId()).getUserName()).isEqualTo("alice");
    }

    /** 按主键删除。 */
    @Test
    void removesById() {
        // given
        UserEo user = newUser("alice", 30);
        userDas.save(user);

        // when
        assertThat(userDas.removeById(user.getId())).isTrue();

        // then
        assertThat(userDas.findById(user.getId())).isNull();
    }

    /** 批量保存中途失败时回滚已插入记录。 */
    @Test
    void saveBatchRollsBackOnFailure() {
        // given
        UserEo first = newUser("peter", 90);
        first.setId(new ObjectId());
        UserEo duplicate = newUser("peter2", 91);
        duplicate.setId(first.getId());

        // when
        assertThatThrownBy(() -> userDas.saveBatch(List.of(first, duplicate)))
                .isInstanceOf(BanyanException.class);

        // then
        // 失败后整批不应残留
        assertThat(userDas.count()).isZero();
    }

    /** 忽略错误的批量保存：重复主键不影响其余记录。 */
    @Test
    void saveBatchIgnoreErrorsKeepsGoing() {
        // given
        UserEo first = newUser("olive", 80);
        first.setId(new ObjectId());
        UserEo duplicate = newUser("olive2", 81);
        duplicate.setId(first.getId());

        // when
        List<UserEo> result = userDas.saveBatchIgnoreErrors(List.of(first, duplicate));

        // then
        assertThat(result).hasSize(2);
        // 重复主键的那条被忽略，只写入 1 条
        assertThat(userDas.count()).isEqualTo(1);
    }

    /** 批量保存。 */
    @Test
    void savesBatch() {
        // given
        List<UserEo> users = List.of(newUser("alice", 30), newUser("bob", 20));

        // when
        userDas.saveBatch(users);

        // then
        assertThat(users).allMatch(user -> user.getId() != null);
        assertThat(userDas.count()).isEqualTo(2);
    }

    /** 动态条件分页。 */
    @Test
    void findPageWithDynamicConditions() {
        // given
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        UserPageQuery query = new UserPageQuery();
        query.setUserNameLike("ali");

        // when
        PageResult<UserEo> page = userDas.findPage(query);

        // then
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getUserName()).isEqualTo("alice");
    }

    /** 无排序时保持稳定顺序。 */
    @Test
    void findPageKeepsStableOrderWithoutSorts() {
        // given
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        // when
        PageResult<UserEo> first = userDas.findPage(new UserPageQuery());
        PageResult<UserEo> second = userDas.findPage(new UserPageQuery());

        // then
        assertThat(first.data()).extracting(UserEo::getId)
                .containsExactlyElementsOf(second.data().stream().map(UserEo::getId).toList());
    }

    /** 流式查询全部。 */
    @Test
    void streamsAll() {
        // given
        userDas.saveBatch(List.of(newUser("alice", 30), newUser("bob", 20)));

        // when
        assertThat(userDas.findAllStream().count()).isEqualTo(2);
    }

    /** 批量按主键更新遵循公共契约：返回 int。 */
    @Test
    void updateBatchByIdFollowsContract() throws NoSuchMethodException {
        // when
        assertThat(AbstractMongoDas.class.getMethod("updateBatchById", Collection.class).getReturnType())
                .isEqualTo(int.class);
    }

    /** 批量更新只写提供的字段，未提供的字段保持原值。 */
    @Test
    void updateBatchByIdUpdatesOnlyProvidedFields() {
        // given
        UserEo first = newUser("alice", 30);
        first.setEmail("alice@example.com");
        UserEo second = newUser("bob", 20);
        second.setEmail("bob@example.com");
        userDas.saveBatch(List.of(first, second));
        UserEo firstPatch = new UserEo();
        firstPatch.setId(first.getId());
        firstPatch.setAge(31);
        UserEo secondPatch = new UserEo();
        secondPatch.setId(second.getId());
        secondPatch.setUserName("bobby");

        // when
        assertThat(userDas.updateBatchById(List.of(firstPatch, secondPatch))).isEqualTo(2);

        // then
        UserEo reloadedFirst = userDas.findById(first.getId());
        assertThat(reloadedFirst.getAge()).isEqualTo(31);
        assertThat(reloadedFirst.getUserName()).isEqualTo("alice");
        assertThat(reloadedFirst.getEmail()).isEqualTo("alice@example.com");
        UserEo reloadedSecond = userDas.findById(second.getId());
        assertThat(reloadedSecond.getUserName()).isEqualTo("bobby");
        assertThat(reloadedSecond.getAge()).isEqualTo(20);
        assertThat(reloadedSecond.getEmail()).isEqualTo("bob@example.com");
    }

    /** 返回数据库报告的实际修改文档数，而不是入参记录数。 */
    @Test
    void updateBatchByIdReturnsModifiedCount() {
        // given
        UserEo changed = newUser("bob", 20);
        UserEo unchanged = newUser("carol", 21);
        userDas.saveBatch(List.of(changed, unchanged));
        UserEo changedPatch = new UserEo();
        changedPatch.setId(changed.getId());
        changedPatch.setAge(22);
        UserEo unchangedPatch = new UserEo();
        unchangedPatch.setId(unchanged.getId());
        unchangedPatch.setUserName("carol");

        // when
        assertThat(userDas.updateBatchById(List.of(changedPatch, unchangedPatch))).isEqualTo(1);
    }

    /** 批量更新：null 入参与缺主键都抛参数异常。 */
    @Test
    void updateBatchByIdRejectsNullAndMissingId() {
        // when
        assertThatThrownBy(() -> userDas.updateBatchById(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userDas.updateBatchById(List.of(newUser("dora", 22))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 批量更新：空集合返回 0。 */
    @Test
    void updateBatchByIdReturnsZeroForEmpty() {
        // when
        assertThat(userDas.updateBatchById(List.of())).isZero();
    }

    /** 批量更新：重复主键与只有主键的记录整批失败。 */
    @Test
    void updateBatchByIdRejectsDuplicateIdAndEmptyContent() {
        // given
        UserEo user = newUser("erin", 23);
        userDas.save(user);
        UserEo duplicate = newUser("erin2", 24);
        duplicate.setId(user.getId());
        UserEo idOnly = new UserEo();
        idOnly.setId(user.getId());

        // when
        assertThatThrownBy(() -> userDas.updateBatchById(List.of(user, duplicate)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userDas.updateBatchById(List.of(idOnly)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** MongoDB 侧不提供单条 SQL 选路重载，也没有列数上限。 */
    @Test
    void exposesNoSqlPathOverloads() {
        // when
        assertThatThrownBy(() -> AbstractMongoDas.class.getMethod(
                "updateBatchById", Collection.class, boolean.class))
                .isInstanceOf(NoSuchMethodException.class);
        assertThat(java.util.Arrays.stream(AbstractMongoDas.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("maxupdatecolumns"))).isTrue();
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
