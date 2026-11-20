package cn.kennylee.banyan.data.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 关闭分页开关后的反证：同一查询不再分页。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.pagination.enabled=false")
@Transactional
class PaginationDisabledProbeIntegrationTest {

    @Autowired
    private PagingProbeMapper pagingProbeMapper;

    /** 关闭分页后返回全部记录，Page 不再生效。 */
    @Test
    void returnsAllRecordsWhenPaginationDisabled() {
        // when
        IPage<UserEo> page = pagingProbeMapper.selectByMinAge(new Page<>(1, 1), 0);

        // then
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getTotal()).isZero();
    }
}
