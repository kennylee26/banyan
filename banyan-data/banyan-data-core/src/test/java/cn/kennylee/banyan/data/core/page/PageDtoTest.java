package cn.kennylee.banyan.data.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 分页入参测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class PageDtoTest {

    /** 默认第一页每页 10 条。 */
    @Test
    void defaultsToFirstPageOfTen() {
        // given
        PageDto page = new PageDto();

        // when
        assertEquals(1, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertNull(page.getSearchCount());
    }

    /** searchCount 保持三态。 */
    @Test
    void searchCountKeepsThreeStates() {
        // given
        PageDto page = new PageDto();

        // when
        assertNull(page.getSearchCount());

        page.setSearchCount(Boolean.TRUE);
        assertEquals(Boolean.TRUE, page.getSearchCount());

        page.setSearchCount(Boolean.FALSE);
        assertEquals(Boolean.FALSE, page.getSearchCount());
    }

    /** 排序项携带字段与方向。 */
    @Test
    void sortItemCarriesFieldAndDirection() {
        // given
        SortDto sort = SortDto.of("create_time", SortDirection.DESC);

        // when
        assertEquals("create_time", sort.getField());
        assertEquals(SortDirection.DESC, sort.getDirection());
    }

    /** 分页入参携带排序项。 */
    @Test
    void pageCarriesSorts() {
        // given
        PageDto page = new PageDto();
        page.setSorts(List.of(SortDto.of("id", SortDirection.ASC)));

        // when
        assertEquals(1, page.getSorts().size());
    }
}
