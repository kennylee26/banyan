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
        IdProvider provider = new UuidIdProvider();

        String first = provider.nextId();
        String second = provider.nextId();

        assertNotNull(first);
        assertFalse(first.isBlank());
        assertNotEquals(first, second);
        assertEquals(32, first.length());
        assertFalse(first.contains("-"));
    }

    /** 默认实现的 Long ID 为正数且递增。 */
    @Test
    void defaultProviderReturnsIncreasingLongIds() {
        IdProvider provider = new UuidIdProvider();

        long first = provider.nextLongId();
        long second = provider.nextLongId();

        assertTrue(first > 0);
        assertTrue(second > first);
    }

    /** 自定义实现可替换默认实现。 */
    @Test
    void customImplementationCanReplaceDefault() {
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

        assertEquals("fixed-id", fixed.nextId());
        assertEquals(1L, fixed.nextLongId());
    }
}
