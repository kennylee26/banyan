package cn.kennylee.banyan.data.mysql.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LIKE 转义的端到端测试：默认配置下 DTO 模糊条件按字面量搜索。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
@Transactional
class LikeEscapeIntegrationTest extends LikeEscapeTestSupport {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    /** % 按字面量匹配。 */
    @Test
    void treatsPercentAsLiteral() {
        // when
        assertThat(userDas.findPage(query("50%")).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");
    }

    /** _ 按字面量匹配。 */
    @Test
    void treatsUnderscoreAsLiteral() {
        // when
        assertThat(userDas.findPage(query("51_a")).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-51_a");
    }

    /** 策略层只覆盖查询 DTO；手写 XML 在全局层关闭时保持原生通配语义。 */
    @Test
    void keepsXmlLikeAsWildcardWhenGlobalLayerDisabled() {
        // when
        assertThat(pagingProbeMapper.selectByNameLike("50%"))
                .extracting(UserEo::getUserName)
                .containsExactlyInAnyOrder("pg-50%off", "pg-50xoff");
    }
}
