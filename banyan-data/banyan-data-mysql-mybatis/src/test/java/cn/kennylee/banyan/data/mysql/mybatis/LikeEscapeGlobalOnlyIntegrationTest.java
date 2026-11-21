package cn.kennylee.banyan.data.mysql.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 只开全局层、关闭策略层：DTO 与 XML 均由全局层完成转义。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class, properties = {
        "banyan.data-mysql.like-escape.enabled=false",
        "banyan.data-mysql.like-escape.inner-interceptor.enabled=true"})
@Transactional
class LikeEscapeGlobalOnlyIntegrationTest extends LikeEscapeTestSupport {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    /** 策略层关闭后，全局层仍保证 DTO 的字面量匹配。 */
    @Test
    void globalLayerEscapesDtoQuery() {
        // when
        assertThat(userDas.findPage(query("50%")).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");
    }

    /** XML 同样由全局层转义。 */
    @Test
    void globalLayerEscapesXmlQuery() {
        // when
        assertThat(pagingProbeMapper.selectByNameLike("50%"))
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");
    }
}
