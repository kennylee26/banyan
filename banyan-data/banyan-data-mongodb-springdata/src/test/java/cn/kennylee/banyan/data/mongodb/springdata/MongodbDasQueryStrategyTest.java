package cn.kennylee.banyan.data.mongodb.springdata;

import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.mongodb.springdata.strategy.MongodbDasQueryStrategy;
import cn.kennylee.banyan.data.mongodb.springdata.utils.MongoDbDasHelper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MongoDB 查询策略测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class MongodbDasQueryStrategyTest {

    /** 各类模糊查询构造正则。 */
    @Test
    void buildsRegexForLikeVariants() {
        // given
        UserQuery query = new UserQuery();
        query.setUserNameLike("ali");
        query.setUserNamePreLike("al");
        query.setUserNameSufLike("ce");

        // when
        Document document = parse(query).getQueryObject();

        // then
        assertThat(document.toJson()).contains("$regularExpression");
    }

    /** 构造 $in 与 $nin。 */
    @Test
    void buildsInAndNin() {
        // given
        UserQuery query = new UserQuery();
        query.setEmailIn(List.of("a@x.com", "b@x.com"));
        query.setEmailNotIn(List.of("c@x.com"));

        // when
        Document document = parse(query).getQueryObject();

        // then
        assertThat(document.toJson()).contains("$in").contains("$nin");
    }

    /** 多条件以 $and 合并。 */
    @Test
    void combinesMultipleConditionsWithAnd() {
        // given
        UserQuery query = new UserQuery();
        query.setAgeGt(18);
        query.setUserNameLike("ali");

        // when
        Document document = parse(query).getQueryObject();

        // then
        assertThat(document.containsKey("$and")).isTrue();
    }

    /** LocalDateTime 转 epoch 毫秒。 */
    @Test
    void convertsLocalDateTimeToEpochMilli() {
        // given
        LocalDateTime time = LocalDateTime.of(2024, 1, 2, 3, 4, 5);

        // when
        Object converted = MongoDbDasHelper.toEpochMilliIfNecessary(time);

        // then
        assertThat(converted).isInstanceOf(Long.class);
        assertThat(converted).isEqualTo(time.toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli());
    }

    private Query parse(PageDto query) {
        return (Query) new MongodbDasQueryStrategy<>(UserEo.class)
                .parseQuery(query, UserEo.class, List.of());
    }

    static class UserQuery extends PageDto {
        private String userNameLike;
        private String userNamePreLike;
        private String userNameSufLike;
        private Integer ageGt;
        private List<String> emailIn;
        private List<String> emailNotIn;

        public String getUserNameLike() { return userNameLike; }
        public void setUserNameLike(String v) { this.userNameLike = v; }
        public String getUserNamePreLike() { return userNamePreLike; }
        public void setUserNamePreLike(String v) { this.userNamePreLike = v; }
        public String getUserNameSufLike() { return userNameSufLike; }
        public void setUserNameSufLike(String v) { this.userNameSufLike = v; }
        public Integer getAgeGt() { return ageGt; }
        public void setAgeGt(Integer v) { this.ageGt = v; }
        public List<String> getEmailIn() { return emailIn; }
        public void setEmailIn(List<String> v) { this.emailIn = v; }
        public List<String> getEmailNotIn() { return emailNotIn; }
        public void setEmailNotIn(List<String> v) { this.emailNotIn = v; }
    }
}
