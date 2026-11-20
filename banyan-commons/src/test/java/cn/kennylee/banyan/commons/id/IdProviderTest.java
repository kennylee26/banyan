package cn.kennylee.banyan.commons.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ID 生成器契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class IdProviderTest {

    /** 默认实现返回非空且唯一的 String ID。 */
    @Test
    void defaultProviderReturnsNonBlankUniqueIds() {
        // given
        IdProvider provider = new UuidIdProvider();

        // when
        String first = provider.nextId();
        String second = provider.nextId();

        // then
        assertNotNull(first);
        assertFalse(first.isBlank());
        assertNotEquals(first, second);
        assertEquals(32, first.length());
        assertFalse(first.contains("-"));
    }

    /** 默认实现的 Long ID 为正数且递增。 */
    @Test
    void defaultProviderReturnsIncreasingLongIds() {
        // given
        IdProvider provider = new UuidIdProvider();

        // when
        long first = provider.nextLongId();
        long second = provider.nextLongId();

        // then
        assertTrue(first > 0);
        assertTrue(second > first);
    }

    /** 自定义实现可替换默认实现。 */
    @Test
    void customImplementationCanReplaceDefault() {
        // given
        IdProvider fixed = new IdProvider() {
            @Override
            public String nextId() {
                return "fixed-id";
            }

            @Override
            public long nextLongId() {
                return 1L;
            }
        };

        // when
        assertEquals("fixed-id", fixed.nextId());
        assertEquals(1L, fixed.nextLongId());
    }
}
