package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.mysql.mybatis.strategy.MyBatisDasQueryStrategy;
import cn.kennylee.banyan.data.mysql.mybatis.wrapper.BanyanQueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查询策略的 LIKE 转义开关测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MyBatisDasQueryStrategyEscapeTest {

    /** 开关开启：值被转义。 */
    @Test
    void escapesWhenEnabled() {
        // when
        BanyanQueryWrapper<UserEo> wrapper = parseWithEscape(true, "50%");

        // then
        assertThat(wrapper.getParamNameValuePairs().values())
                .anySatisfy(value -> assertThat(String.valueOf(value)).contains("\\%"));
    }

    /** 开关关闭：值原样透传，% 仍按通配符处理。 */
    @Test
    void keepsValueRawWhenDisabled() {
        // when
        BanyanQueryWrapper<UserEo> wrapper = parseWithEscape(false, "50%");

        // then
        assertThat(wrapper.getParamNameValuePairs().values())
                .anySatisfy(value -> assertThat(String.valueOf(value)).isEqualTo("%50%%"));
        assertThat(wrapper.getParamNameValuePairs().values())
                .noneSatisfy(value -> assertThat(String.valueOf(value)).contains("\\%"));
    }

    @SuppressWarnings("unchecked")
    private BanyanQueryWrapper<UserEo> parseWithEscape(boolean escapeEnabled, String likeValue) {
        MyBatisDasQueryStrategy<UserEo> strategy = new MyBatisDasQueryStrategy<>(escapeEnabled);
        EscapeQuery query = new EscapeQuery();
        query.setUserNameLike(likeValue);
        BanyanQueryWrapper<UserEo> parsed = (BanyanQueryWrapper<UserEo>) strategy.parseQuery(query, UserEo.class, List.of());
        // MyBatis-Plus 在生成 SQL 段时才惰性填充参数表
        parsed.getSqlSegment();
        return parsed;
    }
}
