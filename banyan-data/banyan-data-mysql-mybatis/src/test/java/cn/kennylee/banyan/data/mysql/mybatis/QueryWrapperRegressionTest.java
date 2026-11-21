package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.mysql.mybatis.wrapper.BanyanQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自研查询容器的回归测试。
 * <p>覆盖历史坑：</p>
 * <ul>
 * <li>{@code e08a6f24}「修复动态分页查询方法」：{@code QueryChainWrapper} 的
 * {@code getSqlSelect()/getCustomSqlSegment()/getSqlSegment()/getExpression()} 抛异常，
 * 不能直接传给 {@code selectPage}；自研容器必须可以直接分页。</li>
 * <li>{@code 1245ab69}「修复排序无效的问题」：空 where 条件下走「无条件」分支会丢排序。</li>
 * <li>原 {@code JoinWrappers.query().selectAll()} 生成全字段列：自研容器不生成别名列，但必须不漏字段。</li>
 * <li>原 14 个 {@code addXxxCriteria} 条件方法：自研容器必须全部可组合。</li>
 * </ul>
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
class QueryWrapperRegressionTest {

    @Autowired
    private UserMapper userMapper;

    /** 容器的 like 是原生透传：传入已转义的值不会被二次加工。 */
    @Test
    void wrapperLikeKeepsValueAsIs() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);

        // when
        wrapper.like("user_name", "50\\%");
        // MyBatis-Plus 在生成 SQL 段时才惰性填充参数表
        wrapper.getSqlSegment();

        // then
        assertThat(wrapper.getParamNameValuePairs().values()).contains("%50\\%%");
    }

    /** 自研容器可直接用于分页。 */
    @Test
    void wrapperCanBeUsedDirectlyBySelectPage() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.like("user_name", "a");

        // when
        IPage<UserEo> page = userMapper.selectPage(new Page<>(1, 10), wrapper);

        // then
        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getTotal()).isPositive();
        // 四个曾被链式代理容器拒绝的方法必须可用
        assertThat(wrapper.getSqlSegment()).isNotBlank();
        assertThat(wrapper.getCustomSqlSegment()).isNotBlank();
        assertThat(wrapper.getExpression()).isNotNull();
        assertThat(wrapper.getSqlSelect()).isNullOrEmpty();
    }

    /** 查询全部列且不带别名。 */
    @Test
    void selectsAllColumnsWithoutAlias() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.eq("id", 1L);

        // when
        UserEo user = userMapper.selectOne(wrapper);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getUserName()).isNotBlank();
        assertThat(user.getAge()).isNotNull();
        assertThat(user.getEmail()).isNotBlank();
    }

    /** 条件方法与排序组合。 */
    @Test
    void combinesConditionMethodsAndOrdering() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.ge("age", 20);
        wrapper.like("user_name", "a");
        wrapper.in("id", List.of(1L, 2L));
        wrapper.orderByDesc("age");
        wrapper.last("limit 10");

        // when
        List<UserEo> users = userMapper.selectList(wrapper);

        // then
        assertThat(users).extracting(UserEo::getUserName).contains("alice");
        assertThat(users).isSortedAccordingTo(
                (left, right) -> Integer.compare(right.getAge(), left.getAge()));
    }

    /** 空条件保留排序与总数。 */
    @Test
    void emptyWhereKeepsSortAndCount() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.orderByDesc("age");

        // when
        IPage<UserEo> page = userMapper.selectPage(new Page<>(1, 10), wrapper);

        // then
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getRecords().get(0).getAge())
                .isGreaterThanOrEqualTo(page.getRecords().get(1).getAge());
    }

    /** 计数与条件一致。 */
    @Test
    void countMatchesCondition() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.ge("age", 25);

        // when
        Long count = userMapper.selectCount(wrapper);

        // then
        assertThat(count).isEqualTo(1);
    }

    /** 支持 lambda 字段引用。 */
    @Test
    void supportsLambdaFieldReference() {
        // given
        BanyanQueryWrapper<UserEo> wrapper = new BanyanQueryWrapper<>(UserEo.class);
        wrapper.eq(UserEo::getUserName, "alice");

        // when
        List<UserEo> users = userMapper.selectList(wrapper);

        // then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserName()).isEqualTo("alice");
    }
}
