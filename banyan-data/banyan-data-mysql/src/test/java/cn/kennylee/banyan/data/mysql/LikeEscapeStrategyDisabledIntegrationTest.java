package cn.kennylee.banyan.data.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 关闭策略层转义后的反证：% 恢复通配语义。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.like-escape.enabled=false")
@Transactional
class LikeEscapeStrategyDisabledIntegrationTest extends LikeEscapeTestSupport {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    /** 关闭后 % 按通配符匹配。 */
    @Test
    void treatsPercentAsWildcardWhenDisabled() {
        // when
        assertThat(userDas.findPage(query("50%")).data())
                .extracting(UserEo::getUserName)
                .containsExactlyInAnyOrder("pg-50%off", "pg-50xoff");
    }

    /** 关闭策略层后，XML 的 like 值同样按通配符匹配。 */
    @Test
    void keepsXmlLikeAsWildcardWhenDisabled() {
        // when
        assertThat(pagingProbeMapper.selectByNameLike("50%"))
                .extracting(UserEo::getUserName)
                .containsExactlyInAnyOrder("pg-50%off", "pg-50xoff");
    }
}
