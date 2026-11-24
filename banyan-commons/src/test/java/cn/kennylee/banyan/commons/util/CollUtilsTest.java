package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集合工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class CollUtilsTest {

    /** 集合判空。 */
    @Test
    void emptyDetection() {
        // when
        assertTrue(CollUtils.isEmpty(null));
        assertTrue(CollUtils.isEmpty(Collections.emptyList()));
        assertFalse(CollUtils.isEmpty(List.of("a")));
    }

    /** 集合非空判断。 */
    @Test
    void notEmptyDetection() {
        // when
        assertFalse(CollUtils.isNotEmpty(null));
        assertFalse(CollUtils.isNotEmpty(Collections.emptyList()));
        assertTrue(CollUtils.isNotEmpty(List.of("a")));
    }

    /** 按固定大小切分集合。 */
    @Test
    void partitionSplitsBySize() {
        // when
        List<List<String>> chunks = CollUtils.partition(List.of("a", "b", "c"), 2);

        // then
        assertEquals(2, chunks.size());
        assertEquals(List.of("a", "b"), chunks.get(0));
        assertEquals(List.of("c"), chunks.get(1));
    }

    /** 转小写集合并去重。 */
    @Test
    void lowerSetNormalizesCase() {
        // given
        List<String> names = List.of("X-Tenant", "x-tenant", "Accept");

        // when
        Set<String> lowered = CollUtils.lowerSet(names);

        // then
        assertEquals(Set.of("x-tenant", "accept"), lowered);
    }

    /** null 输入转空集合。 */
    @Test
    void lowerSetToleratesNull() {
        // when / then
        assertTrue(CollUtils.lowerSet(null).isEmpty());
    }
}
