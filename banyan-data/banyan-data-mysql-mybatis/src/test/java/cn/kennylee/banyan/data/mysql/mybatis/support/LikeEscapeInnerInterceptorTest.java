package cn.kennylee.banyan.data.mysql.mybatis.support;

import cn.kennylee.banyan.data.mysql.mybatis.wrapper.BanyanQueryWrapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局 LIKE 转义拦截器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class LikeEscapeInnerInterceptorTest {

    private final Configuration configuration = new Configuration();

    private final LikeEscapeInnerInterceptor interceptor = new LikeEscapeInnerInterceptor(false);

    /** 顶层 Map 参数中的 like 值被转义。 */
    @Test
    void escapesTopLevelMapParameter() {
        // given
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("keyword", "50%");

        // when
        invoke("SELECT * FROM users WHERE user_name LIKE ?", "keyword", parameter);

        // then
        assertThat(parameter).containsEntry("keyword", "50\\%");
    }

    /** 顶层参数按原始数据整体转义；通配符由 SQL 的 CONCAT 自行拼接。 */
    @Test
    void escapesTopLevelValueAsWhole() {
        // given
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("keyword", "%50_%");

        // when
        invoke("SELECT * FROM users WHERE user_name LIKE CONCAT('%', ?, '%')", "keyword", parameter);

        // then
        assertThat(parameter).containsEntry("keyword", "\\%50\\_\\%");
    }

    /** 条件构造器产生的参数同样被转义，且不重复转义。 */
    @Test
    void escapesWrapperParameter() {
        // given
        BanyanQueryWrapper<Object> wrapper = new BanyanQueryWrapper<>(Object.class);
        wrapper.like("user_name", "50%");
        wrapper.getSqlSegment();
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("ew", wrapper);

        // when
        invoke("SELECT * FROM users WHERE user_name LIKE ?",
                "ew.paramNameValuePairs.MPGENVAL1", parameter);

        // then
        assertThat(wrapper.getParamNameValuePairs()).containsEntry("MPGENVAL1", "%50\\%%");
    }

    /** 单侧通配符（likeLeft / likeRight 产物）只保留该侧通配符。 */
    @Test
    void preservesSingleSideWildcard() {
        // when
        BanyanQueryWrapper<Object> right = new BanyanQueryWrapper<>(Object.class);
        right.likeRight("user_name", "50_");
        right.getSqlSegment();
        Map<String, Object> rightParameter = new HashMap<>();
        rightParameter.put("ew", right);
        invoke("SELECT * FROM users WHERE user_name LIKE ?",
                "ew.paramNameValuePairs.MPGENVAL1", rightParameter);
        assertThat(right.getParamNameValuePairs()).containsEntry("MPGENVAL1", "50\\_%");

        BanyanQueryWrapper<Object> left = new BanyanQueryWrapper<>(Object.class);
        left.likeLeft("user_name", "50_");
        left.getSqlSegment();
        Map<String, Object> leftParameter = new HashMap<>();
        leftParameter.put("ew", left);
        invoke("SELECT * FROM users WHERE user_name LIKE ?",
                "ew.paramNameValuePairs.MPGENVAL1", leftParameter);
        assertThat(left.getParamNameValuePairs()).containsEntry("MPGENVAL1", "%50\\_");
    }

    /** 策略层启用时 wrapper 参数不再由全局层处理，避免重复转义。 */
    @Test
    void skipsWrapperWhenStrategyEscapeEnabled() {
        // given
        BanyanQueryWrapper<Object> wrapper = new BanyanQueryWrapper<>(Object.class);
        wrapper.like("user_name", "50%");
        wrapper.getSqlSegment();
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("ew", wrapper);

        // when
        new LikeEscapeInnerInterceptor(true).beforeQuery(null, null, parameter, null, null,
                boundSql("SELECT * FROM users WHERE user_name LIKE ?",
                        "ew.paramNameValuePairs.MPGENVAL1", parameter));

        // then
        assertThat(wrapper.getParamNameValuePairs()).containsEntry("MPGENVAL1", "%50%%");
    }

    /** 嵌套 bean 形态原样跳过并输出 DEBUG。 */
    @Test
    void skipsNestedBeanAndLogsDebug(CapturedOutput output) {
        // given
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("req", new Request("50%"));
        org.springframework.boot.logging.LoggingSystem loggingSystem =
                org.springframework.boot.logging.LoggingSystem.get(getClass().getClassLoader());
        String loggerName = LikeEscapeInnerInterceptor.class.getName();
        loggingSystem.setLogLevel(loggerName, org.springframework.boot.logging.LogLevel.DEBUG);

        // when
        try {
            invoke("SELECT * FROM users WHERE user_name LIKE ?", "req.keyword", parameter);
        } finally {
            loggingSystem.setLogLevel(loggerName, null);
        }

        // then
        assertThat(((Request) parameter.get("req")).getKeyword()).isEqualTo("50%");
        assertThat(output).contains("不支持的 LIKE 参数形态");
    }

    /** 不含 like 的 SQL 不做任何加工。 */
    @Test
    void ignoresSqlWithoutLike() {
        // given
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("age", "50%");

        // when
        invoke("SELECT * FROM users WHERE age = ?", "age", parameter);

        // then
        assertThat(parameter).containsEntry("age", "50%");
    }

    private void invoke(String sql, String property, Map<String, Object> parameter) {
        interceptor.beforeQuery(null, null, parameter, null, null, boundSql(sql, property, parameter));
    }

    private BoundSql boundSql(String sql, String property, Object parameter) {
        List<ParameterMapping> mappings = List.of(
                new ParameterMapping.Builder(configuration, property, String.class).build());
        return new BoundSql(configuration, sql, mappings, parameter);
    }

    /** 嵌套 bean 参数。 */
    static class Request {

        private final String keyword;

        Request(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }
    }
}
