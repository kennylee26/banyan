package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.mysql.das.IdPolicy;
import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 基础 DAS 端到端测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
@Transactional
class BaseDasTest {

    @Autowired
    private UserDas userDas;

    @Autowired
    private AutoIdUserDas autoIdUserDas;

    /** 保存后可按主键查询。 */
    @Test
    void savesAndFindsById() {
        UserEo user = newUser("carol", 25);

        userDas.save(user);

        assertThat(user.getId()).isNotNull();
        // ASSIGN_ID 由 MyBatis-Plus 分配雪花 ID，不是数据库自增的小数值
        assertThat(user.getId()).isGreaterThan(1000L);
        assertThat(userDas.findById(user.getId()).getUserName()).isEqualTo("carol");
    }

    /** 按主键删除。 */
    @Test
    void removesById() {
        UserEo user = newUser("dave", 26);
        userDas.save(user);

        assertThat(userDas.removeById(user.getId())).isTrue();
        assertThat(userDas.findById(user.getId())).isNull();
    }

    /** 批量保存并分配主键。 */
    @Test
    void savesBatchAndAssignsIds() {
        List<UserEo> users = List.of(newUser("erin", 27), newUser("frank", 28));

        userDas.saveBatch(users);

        assertThat(users).allMatch(user -> user.getId() != null);
        assertThat(userDas.findById(users.get(0).getId()).getUserName()).isEqualTo("erin");
    }

    /** 无参 filter() 返回本体链式容器。 */
    @Test
    void filterReturnsChainWrapper() {
        assertThat(userDas.filter()).isNotNull();
        assertThat(userDas.filter(UserEo.class)).isNotNull();
    }

    /** 雪花策略：单条新增由框架生成 long 主键。 */
    @Test
    void snowflakePolicySingleInsert() {
        userDas.setIdPolicy(IdPolicy.ASSIGN_ID);
        UserEo user = newUser("quinn", 33);

        userDas.save(user);

        // 雪花 ID 远大于数据库自增序列
        assertThat(user.getId()).isNotNull().isGreaterThan(1000L);
        assertThat(userDas.findById(user.getId()).getUserName()).isEqualTo("quinn");
    }

    /** 雪花策略：批量新增每条都拿到独立主键。 */
    @Test
    void snowflakePolicyBatchInsert() {
        userDas.setIdPolicy(IdPolicy.ASSIGN_ID);
        List<UserEo> users = List.of(newUser("rita", 34), newUser("sam", 35));

        userDas.saveBatch(users);

        assertThat(users).allMatch(user -> user.getId() != null && user.getId() > 1000L);
        assertThat(users.get(0).getId()).isNotEqualTo(users.get(1).getId());
    }

    /** 使用方在自己的实体上覆写 @TableId(AUTO) 即可改为数据库自增：单条新增。 */
    @Test
    void autoIdSingleInsert() {
        AutoIdUserEo user = new AutoIdUserEo();
        user.setUserName("tina");
        user.setAge(36);

        autoIdUserDas.save(user);

        assertThat(user.getId()).isNotNull().isLessThan(1000L);
    }

    /** 使用方覆写 @TableId(AUTO)：批量新增由 DB 生成主键并回填。 */
    @Test
    void autoIdBatchInsert() {
        AutoIdUserEo first = new AutoIdUserEo();
        first.setUserName("uma");
        first.setAge(37);
        AutoIdUserEo second = new AutoIdUserEo();
        second.setUserName("victor");
        second.setAge(38);

        autoIdUserDas.saveBatch(List.of(first, second));

        assertThat(first.getId()).isNotNull().isLessThan(1000L);
        assertThat(second.getId()).isNotNull().isLessThan(1000L);
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    /** AUTO 实体走手写多值 INSERT 时也不补 ID，由数据库生成。 */
    @Test
    void autoIdMultiValueInsert() {
        AutoIdUserEo first = new AutoIdUserEo();
        first.setUserName("wendy");
        first.setAge(39);
        AutoIdUserEo second = new AutoIdUserEo();
        second.setUserName("xavier");
        second.setAge(40);

        autoIdUserDas.batchInsertForMySql(List.of(first, second), 100);

        assertThat(first.getId()).isNotNull().isLessThan(1000L);
        assertThat(second.getId()).isNotNull().isLessThan(1000L);
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    /** 最大页码校验在分页入口真正生效。 */
    @Test
    void findPageRejectsPageBeyondMax() {
        UserPageQuery query = new UserPageQuery();
        query.setPageNo(10001);
        query.setPageSize(10);

        assertThatThrownBy(() -> userDas.findPage(query)).isInstanceOf(BanyanException.class);
    }

    /** 动态条件分页。 */
    @Test
    void findPageWithDynamicConditions() {
        UserPageQuery query = new UserPageQuery();
        query.setUserNameLike("ali");

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getUserName()).isEqualTo("alice");
    }

    /** 后模糊分页。 */
    @Test
    void findPageWithSuffixLike() {
        UserPageQuery query = new UserPageQuery();
        query.setUserNameSufLike("ice");

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getUserName()).isEqualTo("alice");
    }

    /** 多值 INSERT 批量保存。 */
    @Test
    void batchInsertForMySqlInsertsAll() {
        List<UserEo> users = List.of(newUser("gina", 31), newUser("henry", 32));

        int inserted = userDas.batchInsertForMySql(users, 100);

        assertThat(inserted).isEqualTo(2);
        assertThat(users).allMatch(user -> user.getId() != null);
        assertThat(userDas.count()).isEqualTo(4);
    }

    /** 批量按主键更新。 */
    @Test
    void updateBatchByIdUpdatesAll() {
        UserEo first = newUser("ivy", 40);
        UserEo second = newUser("jack", 41);
        userDas.save(first);
        userDas.save(second);
        first.setAge(50);
        second.setAge(51);

        assertThat(userDas.updateBatchById(List.of(first, second))).isTrue();
        assertThat(userDas.findById(first.getId()).getAge()).isEqualTo(50);
    }

    /** 批量保存或更新。 */
    @Test
    void saveOrUpdateBatchHandlesBoth() {
        UserEo existing = newUser("kate", 60);
        userDas.save(existing);
        assertThat(existing.getId()).isNotNull();
        existing.setAge(61);
        UserEo created = newUser("leo", 62);

        assertThat(userDas.saveOrUpdateBatch(List.of(existing, created))).isTrue();
        assertThat(userDas.findById(existing.getId()).getAge()).isEqualTo(61);
        assertThat(created.getId()).isNotNull();
        assertThat(userDas.findAll().stream().filter(u -> "kate".equals(u.getUserName())).count()).isEqualTo(1);
        assertThat(userDas.findAll().stream().filter(u -> "leo".equals(u.getUserName())).count()).isEqualTo(1);
    }

    /** 批量按主键删除。 */
    @Test
    void removeBatchByIdsRemovesAll() {
        UserEo first = newUser("mia", 70);
        UserEo second = newUser("nina", 71);
        userDas.save(first);
        userDas.save(second);

        assertThat(userDas.removeBatchByIds(List.of(first.getId(), second.getId()))).isEqualTo(2);
        assertThat(userDas.findById(first.getId())).isNull();
    }

    /** 分页查询前可追加条件。 */
    @Test
    void findPageWithConsumerAppendsCondition() {
        UserPageQuery query = new UserPageQuery();

        PageResult<UserEo> page = userDas.findPage(query, wrapper -> wrapper.ge("age", 25));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).getUserName()).isEqualTo("alice");
    }

    /** 只查总数模式。 */
    @Test
    void findPageOnlyCount() {
        UserPageQuery query = new UserPageQuery();
        query.setSearchCount(Boolean.TRUE);

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.data()).isEmpty();
    }

    /** 原生分页配合 filter 容器。 */
    @Test
    void nativeSelectPageWithFilterContainer() {
        BanyanQueryWrapper<UserEo> wrapper = userDas.filter(UserEo.class);
        wrapper.ge("age", 20);

        IPage<UserEo> page = userDas.selectPage(new Page<>(1, 10), wrapper);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(2);
    }

    private UserEo newUser(String userName, int age) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(age);
        return user;
    }

    static class UserPageQuery extends PageDto {
        private String userNameLike;
        private String userNameSufLike;

        public String getUserNameLike() {
            return userNameLike;
        }

        public void setUserNameLike(String userNameLike) {
            this.userNameLike = userNameLike;
        }

        public String getUserNameSufLike() {
            return userNameSufLike;
        }

        public void setUserNameSufLike(String userNameSufLike) {
            this.userNameSufLike = userNameSufLike;
        }
    }
}
