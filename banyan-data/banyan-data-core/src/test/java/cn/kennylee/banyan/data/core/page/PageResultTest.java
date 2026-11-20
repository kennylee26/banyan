package cn.kennylee.banyan.data.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页结果测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class PageResultTest {

    /** 按总数与条数计算总页数。 */
    @Test
    void computesTotalPageFromTotalAndSize() {
        // given
        PageResult<String> page = new PageResult<>(1, 10, 25, List.of("a", "b"));

        // when
        assertEquals(25, page.total());
        assertEquals(3, page.totalPage());
    }

    /** 空分页无数据。 */
    @Test
    void emptyPageHasNoData() {
        // given
        PageResult<String> page = PageResult.emptyPage();

        // when
        assertEquals(0, page.total());
        assertTrue(page.data() == null || page.data().isEmpty());
    }

    /** 显式总页数被保留。 */
    @Test
    void explicitTotalPageIsKept() {
        // given
        PageResult<String> page = new PageResult<>(2, 10, 25, 3, List.of("c"));

        // when
        assertEquals(2, page.pageNo());
        assertEquals(3, page.totalPage());
    }
}
