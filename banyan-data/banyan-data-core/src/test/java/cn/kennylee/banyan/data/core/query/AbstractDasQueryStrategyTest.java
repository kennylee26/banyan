package cn.kennylee.banyan.data.core.query;

import cn.kennylee.banyan.commons.error.BanyanException;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询策略抽象测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class AbstractDasQueryStrategyTest {

    /** 解析 Bean 非空属性并跳过忽略字段。 */
    @Test
    void parsesBeanNonBlankPropertiesAndSkipsIgnoredProps() {
        DemoQuery query = new DemoQuery();
        query.setName("alice");
        query.setAgeGt(18);
        query.setPageNo(2);

        List<String> criteria = new RecordingStrategy().parse(query);

        assertTrue(criteria.contains("EQ:db_name=alice"));
        assertTrue(criteria.contains("GT:db_age=18"));
        assertTrue(criteria.stream().noneMatch(c -> c.contains("pageNo")));
    }

    /** 解析下划线键的 Map 入参。 */
    @Test
    void parsesMapInputWithSnakeCaseKey() {
        List<String> criteria = new RecordingStrategy()
                .parse(Map.of("user_name", "alice"), Demo.class);

        assertTrue(criteria.contains("EQ:db_userName=alice"));
    }

    /** 未知实体属性报错。 */
    @Test
    void rejectsUnknownEntityProperty() {
        DemoQuery query = new DemoQuery();
        query.setUnknownLike("x");

        BanyanException ex = assertThrows(BanyanException.class,
                () -> new RecordingStrategy().parse(query));
        assertTrue(ex.getMessage().contains("unknown"));
    }

    /** 分派全部 14 个操作符。 */
    @Test
    void dispatchesAllFourteenOperators() {
        DemoQuery query = new DemoQuery();
        query.setName("eq");
        query.setNameNe("ne");
        query.setAgeGt(1);
        query.setAgeGe(2);
        query.setAgeLt(3);
        query.setAgeLe(4);
        query.setNameLike("like");
        query.setNamePreLike("pre");
        query.setNameSufLike("suf");
        query.setNameNotLike("notlike");
        query.setStatusIn(List.of("a"));
        query.setStatusNotIn(List.of("b"));
        query.setEmailIsNull(Boolean.TRUE);
        query.setEmailIsNotNull(Boolean.TRUE);

        List<String> criteria = new RecordingStrategy().parse(query);

        for (String op : new String[]{"EQ", "NE", "GT", "GE", "LT", "LE", "LIKE",
                "PRE_LIKE", "SUF_LIKE", "NOT_LIKE", "IN", "NOT_IN", "IS_NULL", "IS_NOT_NULL"}) {
            assertTrue(criteria.stream().anyMatch(c -> c.startsWith(op + ":")), "missing " + op);
        }
    }

    static class Demo {
        private String name;
        private String userName;
        private Integer age;
        private String status;
        private String email;
    }

    static class DemoQuery {
        private String name;
        private String nameNe;
        private Integer ageGt;
        private Integer ageGe;
        private Integer ageLt;
        private Integer ageLe;
        private String nameLike;
        private String namePreLike;
        private String nameSufLike;
        private String nameNotLike;
        private List<String> statusIn;
        private List<String> statusNotIn;
        private Boolean emailIsNull;
        private Boolean emailIsNotNull;
        private Integer pageNo;
        private String unknownLike;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getNameNe() { return nameNe; }
        public void setNameNe(String v) { this.nameNe = v; }
        public Integer getAgeGt() { return ageGt; }
        public void setAgeGt(Integer v) { this.ageGt = v; }
        public Integer getAgeGe() { return ageGe; }
        public void setAgeGe(Integer v) { this.ageGe = v; }
        public Integer getAgeLt() { return ageLt; }
        public void setAgeLt(Integer v) { this.ageLt = v; }
        public Integer getAgeLe() { return ageLe; }
        public void setAgeLe(Integer v) { this.ageLe = v; }
        public String getNameLike() { return nameLike; }
        public void setNameLike(String v) { this.nameLike = v; }
        public String getNamePreLike() { return namePreLike; }
        public void setNamePreLike(String v) { this.namePreLike = v; }
        public String getNameSufLike() { return nameSufLike; }
        public void setNameSufLike(String v) { this.nameSufLike = v; }
        public String getNameNotLike() { return nameNotLike; }
        public void setNameNotLike(String v) { this.nameNotLike = v; }
        public List<String> getStatusIn() { return statusIn; }
        public void setStatusIn(List<String> v) { this.statusIn = v; }
        public List<String> getStatusNotIn() { return statusNotIn; }
        public void setStatusNotIn(List<String> v) { this.statusNotIn = v; }
        public Boolean getEmailIsNull() { return emailIsNull; }
        public void setEmailIsNull(Boolean v) { this.emailIsNull = v; }
        public Boolean getEmailIsNotNull() { return emailIsNotNull; }
        public void setEmailIsNotNull(Boolean v) { this.emailIsNotNull = v; }
        public Integer getPageNo() { return pageNo; }
        public void setPageNo(Integer v) { this.pageNo = v; }
        public String getUnknownLike() { return unknownLike; }
        public void setUnknownLike(String v) { this.unknownLike = v; }
    }

    static class RecordingStrategy extends AbstractDasQueryStrategy<Object, Demo, List<String>, List<String>> {

        @SuppressWarnings("unchecked")
        List<String> parse(Object query) {
            return (List<String>) parseQuery(query, Demo.class, List.of());
        }

        @SuppressWarnings("unchecked")
        List<String> parse(Object query, Class<Demo> entityClass) {
            return (List<String>) parseQuery(query, entityClass, List.of());
        }

        private List<String> current;

        @Override
        protected List<String> newCriteria(String propName) {
            current = new ArrayList<>();
            return current;
        }

        @Override
        protected List<String> getQuery() {
            return current;
        }

        @Override
        protected String convertDbPropName(String propName) {
            return "db_" + propName;
        }

        @Override
        protected Collection<String> ignoreSearchProps() {
            return Set.of("pageNo");
        }

        @Override
        public DasQueryStrategy<Object, Demo> newInstance() {
            return new RecordingStrategy();
        }

        private void add(List<String> c, String op, String p, Object v) {
            c.add(op + ":" + p + "=" + v);
        }

        @Override protected void addEqCriteria(List<String> c, String p, Object v) { add(c, "EQ", p, v); }
        @Override protected void addNeCriteria(List<String> c, String p, Object v) { add(c, "NE", p, v); }
        @Override protected void addGtCriteria(List<String> c, String p, Object v) { add(c, "GT", p, v); }
        @Override protected void addGeCriteria(List<String> c, String p, Object v) { add(c, "GE", p, v); }
        @Override protected void addLtCriteria(List<String> c, String p, Object v) { add(c, "LT", p, v); }
        @Override protected void addLeCriteria(List<String> c, String p, Object v) { add(c, "LE", p, v); }
        @Override protected void addLikeCriteria(List<String> c, String p, Object v) { add(c, "LIKE", p, v); }
        @Override protected void addPreLikeCriteria(List<String> c, String p, Object v) { add(c, "PRE_LIKE", p, v); }
        @Override protected void addSufLikeCriteria(List<String> c, String p, Object v) { add(c, "SUF_LIKE", p, v); }
        @Override protected void addNotLikeCriteria(List<String> c, String p, Object v) { add(c, "NOT_LIKE", p, v); }
        @Override protected void addInCriteria(List<String> c, String p, Collection<?> v) { add(c, "IN", p, v); }
        @Override protected void addNotInCriteria(List<String> c, String p, Collection<?> v) { add(c, "NOT_IN", p, v); }
        @Override protected void addIsNullCriteria(List<String> c, String p) { add(c, "IS_NULL", p, null); }
        @Override protected void addIsNotNullCriteria(List<String> c, String p) { add(c, "IS_NOT_NULL", p, null); }
    }
}
