package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.SortDto;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页查询的 Spring Boot 集成测试：真实容器 + H2，覆盖多页遍历、排序、条件组合与 searchCount 三态。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
@Transactional
class BaseDasPaginationIntegrationTest {

    /** 本类种子数据条数。 */
    private static final int SEED_SIZE = 25;

    /** 种子数据名称前缀，用于把查询范围限定在本类数据内。 */
    private static final String NAME_PREFIX = "pg-";

    @Autowired
    private UserDas userDas;

    @BeforeEach
    void seed() {
        List<UserEo> users = new ArrayList<>(SEED_SIZE);
        for (int i = 0; i < SEED_SIZE; i++) {
            UserEo user = new UserEo();
            user.setUserName(String.format("%s%02d", NAME_PREFIX, i));
            user.setAge(20 + i);
            user.setCity(i % 2 == 0 ? "gz" : "sz");
            user.setEmail(user.getUserName() + "@example.com");
            users.add(user);
        }
        userDas.saveBatch(users);
    }

    /** 逐页遍历：不重不漏，顺序按排序项稳定。 */
    @Test
    void paginatesAllRowsWithoutDuplicatesOrGaps() {
        List<Long> ids = new ArrayList<>(SEED_SIZE);
        List<Integer> ages = new ArrayList<>(SEED_SIZE);
        for (int pageNo = 1; pageNo <= 3; pageNo++) {
            PaginationQuery query = baseQuery();
            query.setPageNo(pageNo);
            query.setPageSize(10);
            query.setSorts(List.of(SortDto.of("age", "asc")));

            PageResult<UserEo> page = userDas.findPage(query);

            assertThat(page.total()).isEqualTo(SEED_SIZE);
            assertThat(page.totalPage()).isEqualTo(3);
            page.data().forEach(user -> {
                ids.add(user.getId());
                ages.add(user.getAge());
            });
        }

        assertThat(ids).hasSize(SEED_SIZE).doesNotHaveDuplicates();
        assertThat(ages).isSortedAccordingTo(Comparator.naturalOrder());
    }

    /** 条件组合 + 排序 + 单页取全量。 */
    @Test
    void combinesConditionsWithSorting() {
        PaginationQuery query = baseQuery();
        query.setAgeGe(30);
        query.setCity("gz");
        query.setPageSize(100);
        query.setSorts(List.of(SortDto.of("age", "desc")));

        PageResult<UserEo> page = userDas.findPage(query);

        // gz 为偶数行（i=0,2,...,24），age=20+i ≥ 30 即 i ≥ 10，共 8 条
        assertThat(page.total()).isEqualTo(8);
        assertThat(page.data()).extracting(UserEo::getAge)
                .containsExactly(44, 42, 40, 38, 36, 34, 32, 30);
    }

    /** 多字段排序：先按城市升序，同城内按年龄降序。 */
    @Test
    void appliesMultiFieldSorting() {
        PaginationQuery query = baseQuery();
        query.setPageSize(100);
        query.setSorts(List.of(SortDto.of("city", "asc"), SortDto.of("age", "desc")));

        PageResult<UserEo> page = userDas.findPage(query);

        List<String> cities = page.data().stream().map(UserEo::getCity).toList();
        assertThat(cities).hasSize(SEED_SIZE);
        assertThat(cities.subList(0, 13)).containsOnly("gz");
        assertThat(cities.subList(13, SEED_SIZE)).containsOnly("sz");
        assertThat(page.data().stream().filter(user -> "gz".equals(user.getCity())).toList())
                .extracting(UserEo::getAge)
                .containsExactly(44, 42, 40, 38, 36, 34, 32, 30, 28, 26, 24, 22, 20);
    }

    /** searchCount 三态：null 查总数与数据，true 仅查总数，false 仅查数据。 */
    @Test
    void honorsSearchCountTriState() {
        PaginationQuery both = baseQuery();
        PageResult<UserEo> full = userDas.findPage(both);
        assertThat(full.total()).isEqualTo(SEED_SIZE);
        assertThat(full.data()).hasSize(PageDto.DEFAULT_PAGE_SIZE);

        PaginationQuery countOnlyQuery = baseQuery();
        countOnlyQuery.setSearchCount(Boolean.TRUE);
        PageResult<UserEo> countOnly = userDas.findPage(countOnlyQuery);
        assertThat(countOnly.total()).isEqualTo(SEED_SIZE);
        assertThat(countOnly.data()).isEmpty();

        PaginationQuery dataOnlyQuery = baseQuery();
        dataOnlyQuery.setSearchCount(Boolean.FALSE);
        PageResult<UserEo> dataOnly = userDas.findPage(dataOnlyQuery);
        assertThat(dataOnly.total()).isZero();
        assertThat(dataOnly.data()).hasSize(PageDto.DEFAULT_PAGE_SIZE);
    }

    /** pageNo / pageSize 显式为 null 时使用默认值。 */
    @Test
    void appliesDefaultPagingWhenPageFieldsAreNull() {
        PaginationQuery query = baseQuery();
        query.setPageNo(null);
        query.setPageSize(null);

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.pageNo()).isEqualTo(PageDto.DEFAULT_PAGE_NO);
        assertThat(page.pageSize()).isEqualTo(PageDto.DEFAULT_PAGE_SIZE);
        assertThat(page.data()).hasSize(PageDto.DEFAULT_PAGE_SIZE);
    }

    /** 超出末页：总数正确、数据为空。 */
    @Test
    void returnsEmptyDataBeyondLastPage() {
        PaginationQuery query = baseQuery();
        query.setPageNo(4);
        query.setPageSize(10);

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isEqualTo(SEED_SIZE);
        assertThat(page.data()).isEmpty();
    }

    /** 条件不命中：总数为 0、数据为空、总页数为 0。 */
    @Test
    void returnsEmptyResultWhenNoMatch() {
        PaginationQuery query = baseQuery();
        query.setUserNameLike("no-such-prefix-");

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isZero();
        assertThat(page.data()).isEmpty();
        assertThat(page.totalPage()).isZero();
    }

    /** 未指定排序时回退到主键降序，保证分页顺序可重复。 */
    @Test
    void fallsBackToPrimaryKeyDescendingWhenNoSorts() {
        PaginationQuery query = baseQuery();
        query.setPageSize(5);

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.data()).extracting(UserEo::getId)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    /** 集合条件（IN）与分页组合。 */
    @Test
    void filtersByCollectionCondition() {
        PaginationQuery query = baseQuery();
        query.setCityIn(List.of("sz"));
        query.setPageSize(100);

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isEqualTo(12);
        assertThat(page.data()).allMatch(user -> "sz".equals(user.getCity()));
    }

    /** 空集合的 IN 条件返回 0 行，不是全表或报错。 */
    @Test
    void emptyInConditionReturnsNoRows() {
        PaginationQuery query = baseQuery();
        query.setCityIn(List.of());

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.total()).isZero();
        assertThat(page.data()).isEmpty();
    }

    /** 排序字段支持实体属性名（驼峰），内部映射为数据库列名。 */
    @Test
    void acceptsCamelCaseSortField() {
        PaginationQuery query = baseQuery();
        query.setPageSize(100);
        query.setSorts(List.of(SortDto.of("userName", "asc")));

        PageResult<UserEo> page = userDas.findPage(query);

        assertThat(page.data()).extracting(UserEo::getUserName)
                .isSortedAccordingTo(Comparator.naturalOrder());
    }

    /** LIKE 中的 % 与 _ 按字面量匹配，不被当作通配符。 */
    @Test
    void treatsLikeWildcardsAsLiterals() {
        userDas.saveBatch(List.of(
                newUser("pg-50%off"),
                newUser("pg-50xoff"),
                newUser("pg-51_a"),
                newUser("pg-51xa")));

        PaginationQuery percentQuery = baseQuery();
        percentQuery.setUserNameLike("50%");
        assertThat(userDas.findPage(percentQuery).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");

        PaginationQuery underscoreQuery = baseQuery();
        underscoreQuery.setUserNameLike("51_a");
        assertThat(userDas.findPage(underscoreQuery).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-51_a");
    }

    private UserEo newUser(String userName) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(20);
        return user;
    }

    private PaginationQuery baseQuery() {
        PaginationQuery query = new PaginationQuery();
        query.setUserNameLike(NAME_PREFIX);
        return query;
    }

    /** 分页查询入参：条件字段后缀决定操作符，等值条件字段名直接用属性名。 */
    @Getter
    @Setter
    static class PaginationQuery extends PageDto {

        private String userNameLike;

        private Integer ageGe;

        private String city;

        private List<String> cityIn;
    }
}
