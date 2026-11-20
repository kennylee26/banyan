package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.mysql.strategy.MyBatisDasQueryStrategy;
import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis 查询策略测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class MyBatisDasQueryStrategyTest {

    /** 驼峰属性转下划线列名。 */
    @Test
    void convertsCamelCasePropertyToUnderlineColumn() {
        // given
        UserQuery query = new UserQuery();
        query.setUserNameLike("ali");

        // when
        BanyanQueryWrapper<UserEo> wrapper = parse(query);

        // then
        assertThat(wrapper.getSqlSegment()).contains("user_name").doesNotContain("userName");
    }

    /** 跳过分页字段。 */
    @Test
    void skipsPagingFields() {
        // given
        UserQuery query = new UserQuery();
        query.setUserNameLike("ali");
        query.setPageNo(3);
        query.setPageSize(20);

        // when
        BanyanQueryWrapper<UserEo> wrapper = parse(query);

        // then
        assertThat(wrapper.getSqlSegment()).doesNotContain("page_no").doesNotContain("page_size");
    }

    /** 落地全部 14 个操作符。 */
    @Test
    void landsAllFourteenOperators() {
        // given
        AllOperatorsQuery query = new AllOperatorsQuery();
        query.setUserName("eq");
        query.setUserNameNe("ne");
        query.setAgeGt(1);
        query.setAgeGe(2);
        query.setAgeLt(3);
        query.setAgeLe(4);
        query.setUserNameLike("like");
        query.setUserNamePreLike("pre");
        query.setUserNameSufLike("suf");
        query.setUserNameNotLike("notlike");
        query.setEmailIn(List.of("a"));
        query.setEmailNotIn(List.of("b"));
        query.setEmailIsNull(Boolean.TRUE);
        query.setEmailIsNotNull(Boolean.TRUE);

        // when
        BanyanQueryWrapper<UserEo> wrapper = parse(query, AllOperatorsQuery.class);
        String sql = wrapper.getSqlSegment();

        // then
        assertThat(sql).contains("user_name =");
        assertThat(sql).contains("user_name <>");
        assertThat(sql).contains("age >");
        assertThat(sql).contains("age >=");
        assertThat(sql).contains("age <");
        assertThat(sql).contains("age <=");
        assertThat(sql).contains("user_name LIKE");
        assertThat(sql).contains("user_name NOT LIKE");
        assertThat(sql).contains("email IN");
        assertThat(sql).contains("email NOT IN");
        assertThat(sql).contains("email IS NULL");
        assertThat(sql).contains("email IS NOT NULL");
    }

    @SuppressWarnings("unchecked")
    private BanyanQueryWrapper<UserEo> parse(PageDto query) {
        return (BanyanQueryWrapper<UserEo>) new MyBatisDasQueryStrategy<>(UserEo.class)
                .parseQuery(query, UserEo.class, List.of());
    }

    @SuppressWarnings("unchecked")
    private BanyanQueryWrapper<UserEo> parse(PageDto query, Class<?> dtoType) {
        return (BanyanQueryWrapper<UserEo>) new MyBatisDasQueryStrategy<>(UserEo.class)
                .parseQuery(query, UserEo.class, List.of());
    }

    static class UserQuery extends PageDto {
        private String userNameLike;

        public String getUserNameLike() {
            return userNameLike;
        }

        public void setUserNameLike(String userNameLike) {
            this.userNameLike = userNameLike;
        }
    }

    static class AllOperatorsQuery extends PageDto {
        private String userName;
        private String userNameNe;
        private Integer ageGt;
        private Integer ageGe;
        private Integer ageLt;
        private Integer ageLe;
        private String userNameLike;
        private String userNamePreLike;
        private String userNameSufLike;
        private String userNameNotLike;
        private List<String> emailIn;
        private List<String> emailNotIn;
        private Boolean emailIsNull;
        private Boolean emailIsNotNull;

        public String getUserName() { return userName; }
        public void setUserName(String v) { this.userName = v; }
        public String getUserNameNe() { return userNameNe; }
        public void setUserNameNe(String v) { this.userNameNe = v; }
        public Integer getAgeGt() { return ageGt; }
        public void setAgeGt(Integer v) { this.ageGt = v; }
        public Integer getAgeGe() { return ageGe; }
        public void setAgeGe(Integer v) { this.ageGe = v; }
        public Integer getAgeLt() { return ageLt; }
        public void setAgeLt(Integer v) { this.ageLt = v; }
        public Integer getAgeLe() { return ageLe; }
        public void setAgeLe(Integer v) { this.ageLe = v; }
        public String getUserNameLike() { return userNameLike; }
        public void setUserNameLike(String v) { this.userNameLike = v; }
        public String getUserNamePreLike() { return userNamePreLike; }
        public void setUserNamePreLike(String v) { this.userNamePreLike = v; }
        public String getUserNameSufLike() { return userNameSufLike; }
        public void setUserNameSufLike(String v) { this.userNameSufLike = v; }
        public String getUserNameNotLike() { return userNameNotLike; }
        public void setUserNameNotLike(String v) { this.userNameNotLike = v; }
        public List<String> getEmailIn() { return emailIn; }
        public void setEmailIn(List<String> v) { this.emailIn = v; }
        public List<String> getEmailNotIn() { return emailNotIn; }
        public void setEmailNotIn(List<String> v) { this.emailNotIn = v; }
        public Boolean getEmailIsNull() { return emailIsNull; }
        public void setEmailIsNull(Boolean v) { this.emailIsNull = v; }
        public Boolean getEmailIsNotNull() { return emailIsNotNull; }
        public void setEmailIsNotNull(Boolean v) { this.emailIsNotNull = v; }
    }
}
