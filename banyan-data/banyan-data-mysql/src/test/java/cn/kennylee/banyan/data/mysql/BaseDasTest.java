package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.id.IdProvider;
import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mysql.das.BaseDas;
import cn.kennylee.banyan.data.mysql.das.IdPolicy;
import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
@ExtendWith(OutputCaptureExtension.class)
class BaseDasTest {

    @Autowired
    private UserDas userDas;

    @Autowired
    private AutoIdUserDas autoIdUserDas;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DasQueryStrategyRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

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

    /** 指定 batchSize 的批量保存同样按实体策略补主键，不退化为数据库自增。 */
    @Test
    void saveBatchWithBatchSizeKeepsAssignedIds() {
        List<UserEo> users = List.of(newUser("yan", 80), newUser("zoe", 81));

        userDas.saveBatch(users, 100);

        assertThat(users).allMatch(user -> user.getId() != null && user.getId() > 1000L);
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

    /** 批量按主键更新遵循公共契约：返回 int。 */
    @Test
    void updateBatchByIdFollowsContract() throws NoSuchMethodException {
        assertThat(BaseDas.class.getMethod("updateBatchById", Collection.class).getReturnType())
                .isEqualTo(int.class);
    }

    /** 批量按主键更新：单条 SQL 路径返回各分片数据库原生计数的累加值。 */
    @Test
    void updateBatchByIdReturnsNativeCount() {
        UserEo first = newUser("ivy", 40);
        UserEo second = newUser("jack", 41);
        userDas.save(first);
        userDas.save(second);
        first.setAge(50);
        second.setAge(51);

        assertThat(userDas.updateBatchById(List.of(first, second))).isEqualTo(2);
        assertThat(userDas.findById(first.getId()).getAge()).isEqualTo(50);
    }

    /** batchSize 同时作用于单条 SQL 分片与降级路径。 */
    @Test
    void updateBatchByIdAppliesBatchSizeToBothPaths() {
        List<UserEo> users = List.of(newUser("tom", 70), newUser("uma", 71), newUser("vic", 72));
        userDas.saveBatch(users);
        users.forEach(user -> user.setAge(user.getAge() + 1));

        // 单条 SQL 路径：3 行按 batchSize=2 分两片，返回值是两片计数之和
        assertThat(userDas.updateBatchById(users, 2)).isEqualTo(3);
        assertThat(userDas.findById(users.get(2).getId()).getAge()).isEqualTo(73);

        // 降级路径：同一 batchSize 交给框架批量，返回降级哨兵
        DialectControlledUserDas fallbackDas = wire(new DialectControlledUserDas(userMapper, registry, false));
        assertThat(fallbackDas.updateBatchById(users, 2)).isEqualTo(-1);
        assertThat(fallbackDas.findById(users.get(0).getId()).getAge()).isEqualTo(71);
    }

    /** 值未变化时计数取自数据库原生报告（H2 把未变化的行也计入 executeUpdate）。 */
    @Test
    void updateBatchByIdReturnsNativeCountForUnchangedRows() {
        UserEo first = newUser("wes", 73);
        UserEo second = newUser("xan", 74);
        userDas.save(first);
        userDas.save(second);
        UserEo firstPatch = new UserEo();
        firstPatch.setId(first.getId());
        firstPatch.setUserName("wes");
        UserEo secondPatch = new UserEo();
        secondPatch.setId(second.getId());
        secondPatch.setUserName("xan");

        assertThat(userDas.updateBatchById(List.of(firstPatch, secondPatch))).isEqualTo(2);
    }

    /** 批量更新：null 入参抛参数异常。 */
    @Test
    void updateBatchByIdRejectsNull() {
        assertThatThrownBy(() -> userDas.updateBatchById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 批量更新：空集合返回 0 且不产生数据库操作。 */
    @Test
    void updateBatchByIdReturnsZeroForEmpty() {
        long before = userDas.count();

        assertThat(userDas.updateBatchById(List.of())).isZero();
        assertThat(userDas.count()).isEqualTo(before);
    }

    /** 批量更新：缺主键时整批不执行。 */
    @Test
    void updateBatchByIdRejectsMissingId() {
        UserEo valid = newUser("nora", 44);
        userDas.save(valid);
        UserEo invalid = newUser("oscar", 45);
        valid.setAge(99);

        assertThatThrownBy(() -> userDas.updateBatchById(List.of(valid, invalid)))
                .isInstanceOf(IllegalArgumentException.class);
        // 合法记录也不能被更新：整批校验在写库之前完成
        assertThat(userDas.findById(valid.getId()).getAge()).isEqualTo(44);
    }

    /** 批量更新：重复主键整批失败。 */
    @Test
    void updateBatchByIdRejectsDuplicateId() {
        UserEo first = newUser("pam", 46);
        userDas.save(first);
        UserEo duplicate = newUser("pam2", 47);
        duplicate.setId(first.getId());

        assertThatThrownBy(() -> userDas.updateBatchById(List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 批量更新：只有主键、其余字段全 null 的记录视为无效。 */
    @Test
    void updateBatchByIdRejectsRecordWithoutUpdatableFields() {
        UserEo user = newUser("quy", 48);
        userDas.save(user);
        UserEo idOnly = new UserEo();
        idOnly.setId(user.getId());

        assertThatThrownBy(() -> userDas.updateBatchById(List.of(idOnly)))
                .isInstanceOf(IllegalArgumentException.class);
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

    /** 修复方言判定后，H2 上的单参 saveBatch 真正走多值 INSERT：手写路径会按 IdProvider 补主键。 */
    @Test
    void saveBatchTakesMultiValueInsertPathOnH2(CapturedOutput output) {
        UserDas localDas = wire(new UserDas(userMapper, registry));
        RecordingIdProvider idProvider = new RecordingIdProvider();
        localDas.setIdProvider(idProvider);

        localDas.saveBatch(List.of(newUser("dora", 50), newUser("evan", 51)));

        // 直接断言路径日志，不再只靠 IdProvider 调用次数间接推断
        assertThat(output).contains("使用单条多值 INSERT");
        assertThat(idProvider.longIdCalls).isEqualTo(2);
    }

    /** 扩展点：覆写后把默认不支持的方言判为支持，批量保存改走多值 INSERT。 */
    @Test
    void overridingExtensionEnablesMultiValueInsert() {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, true));
        RecordingIdProvider idProvider = new RecordingIdProvider();
        das.setIdProvider(idProvider);

        das.saveBatch(List.of(newUser("fay", 52), newUser("gil", 53)));

        assertThat(das.probedDbType).isEqualTo(DbType.H2);
        assertThat(das.probedJdbcUrl).startsWith("jdbc:h2:");
        assertThat(idProvider.longIdCalls).isEqualTo(2);
    }

    /** 扩展点：覆写把 H2 判为不支持，批量保存改走框架批量。 */
    @Test
    void overridingExtensionDisablesDefaultDialect() {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, false));
        RecordingIdProvider idProvider = new RecordingIdProvider();
        das.setIdProvider(idProvider);

        List<UserEo> users = das.saveBatch(List.of(newUser("hal", 54), newUser("ida", 55)));

        assertThat(das.probes).isEqualTo(1);
        assertThat(idProvider.longIdCalls).isZero();
        assertThat(users).allMatch(user -> user.getId() != null);
    }

    /** useBatchSql=false 直接走框架批量：不发生方言探测。 */
    @Test
    void saveBatchWithFalseSkipsDialectProbe() {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, true));
        RecordingIdProvider idProvider = new RecordingIdProvider();
        das.setIdProvider(idProvider);

        das.saveBatch(List.of(newUser("jen", 56), newUser("kim", 57)), false);

        assertThat(das.probes).isZero();
        assertThat(idProvider.longIdCalls).isZero();
    }

    /** 单参 saveBatch 等价于 useBatchSql=true。 */
    @Test
    void singleArgSaveBatchEqualsTrue() {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, true));

        das.saveBatch(List.of(newUser("lou", 58), newUser("max", 59)));

        assertThat(das.probes).isEqualTo(1);
    }

    /** useBatchSql=true 但方言不支持：降级到框架批量并输出 DEBUG。 */
    @Test
    void saveBatchFallsBackWhenDialectUnsupported(CapturedOutput output) {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, false));
        RecordingIdProvider idProvider = new RecordingIdProvider();
        das.setIdProvider(idProvider);

        das.saveBatch(List.of(newUser("ned", 60), newUser("oli", 61)), true);

        assertThat(idProvider.longIdCalls).isZero();
        assertThat(output).contains("降级");
    }

    /** useBatchSql=false 时批量更新也不做方言探测。 */
    @Test
    void updateBatchByIdWithFalseSkipsDialectProbe() {
        DialectControlledUserDas das = wire(new DialectControlledUserDas(userMapper, registry, true));
        UserEo user = newUser("pia", 62);
        das.save(user);
        user.setAge(63);

        assertThat(das.updateBatchById(List.of(user), false)).isEqualTo(-1);
        assertThat(das.probes).isZero();
    }

    /** 字段并集超过上限：整批降级到框架批量，两行各更新一次且互不覆盖。 */
    @Test
    void updateBatchByIdFallsBackWhenFieldUnionExceedsLimit(CapturedOutput output) {
        UserEo first = fullUser("quy", 64);
        UserEo second = fullUser("rita", 66);
        userDas.save(first);
        userDas.save(second);
        first.setAge(65);
        first.setCity("shenzhen");
        second.setNickName("rita-new");
        second.setStatus(9);

        assertThat(userDas.updateBatchById(List.of(first, second), true)).isEqualTo(-1);
        assertThat(output).contains("字段并集");
        // 整批降级后每行按本行值更新一次，没有重复更新或互相覆盖
        UserEo reloadedFirst = userDas.findById(first.getId());
        assertThat(reloadedFirst.getAge()).isEqualTo(65);
        assertThat(reloadedFirst.getCity()).isEqualTo("shenzhen");
        assertThat(reloadedFirst.getNickName()).isEqualTo("quy-nick");
        UserEo reloadedSecond = userDas.findById(second.getId());
        assertThat(reloadedSecond.getNickName()).isEqualTo("rita-new");
        assertThat(reloadedSecond.getStatus()).isEqualTo(9);
        assertThat(reloadedSecond.getCity()).isEqualTo("guangzhou");
    }

    /** CASE WHEN 异构非 null 字段：每行只更新自身提供的列，其余列保持原值。 */
    @Test
    void updateBatchByIdKeepsUnprovidedColumns() {
        UserEo first = fullUser("rae", 66);
        UserEo second = fullUser("sid", 67);
        userDas.save(first);
        userDas.save(second);
        UserEo firstPatch = new UserEo();
        firstPatch.setId(first.getId());
        firstPatch.setAge(68);
        UserEo secondPatch = new UserEo();
        secondPatch.setId(second.getId());
        secondPatch.setCity("shenzhen");
        secondPatch.setStatus(9);

        assertThat(userDas.updateBatchById(List.of(firstPatch, secondPatch), true)).isEqualTo(2);

        UserEo reloadedFirst = userDas.findById(first.getId());
        assertThat(reloadedFirst.getAge()).isEqualTo(68);
        assertThat(reloadedFirst.getCity()).isEqualTo("guangzhou");
        assertThat(reloadedFirst.getNickName()).isEqualTo("rae-nick");
        UserEo reloadedSecond = userDas.findById(second.getId());
        assertThat(reloadedSecond.getAge()).isEqualTo(67);
        assertThat(reloadedSecond.getCity()).isEqualTo("shenzhen");
        assertThat(reloadedSecond.getStatus()).isEqualTo(9);
    }

    /** 不提供「出错忽略、继续执行」的批量更新模式。 */
    @Test
    void offersNoIgnoreErrorsUpdateMode() {
        assertThat(BaseDas.class.getMethods())
                .noneMatch(method -> method.getName().startsWith("update")
                        && method.getName().toLowerCase().contains("ignoreerror"));
    }

    /** 手动构造的 DAS 需要注入应用上下文才能取到 SqlSessionFactory。 */
    private <D extends BaseDas<UserEo, Long>> D wire(D das) {
        das.setApplicationContext(applicationContext);
        return das;
    }

    /** 记录补主键调用的 ID 生成器：只有手写 SQL 路径才会用到。 */
    private static class RecordingIdProvider implements IdProvider {

        private int longIdCalls;

        @Override
        public String nextId() {
            return UUID.randomUUID().toString();
        }

        @Override
        public long nextLongId() {
            longIdCalls++;
            return 900000L + longIdCalls;
        }
    }

    private UserEo newUser(String userName, int age) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(age);
        return user;
    }

    /** 7 个可更新字段全部非 null，用于触发字段并集上限。 */
    private UserEo fullUser(String userName, int age) {
        UserEo user = newUser(userName, age);
        user.setEmail(userName + "@example.com");
        user.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        user.setNickName(userName + "-nick");
        user.setCity("guangzhou");
        user.setStatus(1);
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
