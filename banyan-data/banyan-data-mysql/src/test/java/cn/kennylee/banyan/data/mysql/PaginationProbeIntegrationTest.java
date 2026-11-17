package cn.kennylee.banyan.data.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页自动装配的端到端测试：自定义 XML 查询首参传 Page 即分页。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
@Transactional
class PaginationProbeIntegrationTest {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    /** 默认装配后，纯业务 SQL 无需手写分页。 */
    @Test
    void paginatesCustomXmlQueryWithoutManualLimit() {
        IPage<UserEo> page = pagingProbeMapper.selectByMinAge(new Page<>(1, 1), 0);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
    }

    /** 第二页取到剩余记录。 */
    @Test
    void returnsSecondPage() {
        IPage<UserEo> page = pagingProbeMapper.selectByMinAge(new Page<>(2, 1), 0);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getUserName()).isEqualTo("bob");
    }
}
