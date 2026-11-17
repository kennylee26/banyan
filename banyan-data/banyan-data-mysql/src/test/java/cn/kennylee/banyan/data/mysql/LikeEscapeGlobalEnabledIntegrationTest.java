package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.mysql.support.LikeEscapeInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局层与策略层同时开启：按职责分工，同一原始值只转义一次。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.like-escape.inner-interceptor.enabled=true")
@Transactional
class LikeEscapeGlobalEnabledIntegrationTest extends LikeEscapeTestSupport {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    /** 全局层开启时 %, _ 仍按字面量匹配，且没有双重转义。 */
    @Test
    void escapesExactlyOnce() {
        assertThat(userDas.findPage(query("50%")).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");
        assertThat(userDas.findPage(query("51_a")).data())
                .extracting(UserEo::getUserName)
                .containsExactly("pg-51_a");
    }

    /** 手写 XML 由全局层转义。 */
    @Test
    void escapesXmlLikeByGlobalLayer() {
        assertThat(pagingProbeMapper.selectByNameLike("50%"))
                .extracting(UserEo::getUserName)
                .containsExactly("pg-50%off");
    }

    /** 容器顺序：LIKE 转义在分页之前。 */
    @Test
    void registersLikeEscapeBeforePagination() {
        List<InnerInterceptor> interceptors = mybatisPlusInterceptor.getInterceptors();
        assertThat(interceptors.get(0)).isInstanceOf(LikeEscapeInnerInterceptor.class);
        assertThat(interceptors.get(1)).isInstanceOf(PaginationInnerInterceptor.class);
    }
}
