package cn.kennylee.banyan.data.core.validation;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.data.core.page.PageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 分页入口最大页码校验测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class PageLimitTest {

    /** 子类 DTO 继承注解后同样生效。 */
    @Test
    void rejectsPageBeyondMaxOnSubclass() {
        PageDto page = new PageDto();
        page.setPageNo(10001);
        page.setPageSize(10);

        BanyanException exception = assertThrows(BanyanException.class, () -> PageLimit.validate(page));
        assertEquals(ErrorCode.MAX_PAGE_EXCEEDED, exception.getErrorCode());
    }

    /** 正常页码放行。 */
    @Test
    void acceptsNormalPage() {
        PageDto page = new PageDto();
        page.setPageNo(1);
        page.setPageSize(10);

        assertDoesNotThrow(() -> PageLimit.validate(page));
    }
}
