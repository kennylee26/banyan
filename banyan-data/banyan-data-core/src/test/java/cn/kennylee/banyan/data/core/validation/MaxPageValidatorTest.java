package cn.kennylee.banyan.data.core.validation;

import cn.kennylee.banyan.data.core.page.PageDto;
import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 最大页码校验测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class MaxPageValidatorTest {

    /** 超过最大页码被拒绝。 */
    @Test
    void rejectsPageBeyondMaxAllowed() {
        // given
        MaxPageValidator validator = validator(100000);

        PageDto page = new PageDto();
        page.setPageNo(10001);
        page.setPageSize(10);

        // when
        assertFalse(validator.isValid(page, null));
    }

    /** 最大页码被接受。 */
    @Test
    void acceptsPageAtMaxAllowed() {
        // given
        MaxPageValidator validator = validator(100000);

        PageDto page = new PageDto();
        page.setPageNo(10000);
        page.setPageSize(10);

        // when
        assertTrue(validator.isValid(page, null));
    }

    /** null 值通过校验。 */
    @Test
    void acceptsNullValue() {
        // when
        assertTrue(validator(100000).isValid(null, null));
    }

    private MaxPageValidator validator(long totalRecords) {
        MaxPageValidator validator = new MaxPageValidator();
        validator.initialize(new DynamicMaxPage() {
            @Override
            public String message() {
                return "页码超出最大允许值";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public String pageField() {
                return "pageNo";
            }

            @Override
            public String sizeField() {
                return "pageSize";
            }

            @Override
            public long totalRecords() {
                return totalRecords;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DynamicMaxPage.class;
            }
        });
        return validator;
    }
}
