package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

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
        assertTrue(CollUtils.isEmpty(null));
        assertTrue(CollUtils.isEmpty(Collections.emptyList()));
        assertFalse(CollUtils.isEmpty(List.of("a")));
    }

    /** 集合非空判断。 */
    @Test
    void notEmptyDetection() {
        assertFalse(CollUtils.isNotEmpty(null));
        assertFalse(CollUtils.isNotEmpty(Collections.emptyList()));
        assertTrue(CollUtils.isNotEmpty(List.of("a")));
    }

    /** 按固定大小切分集合。 */
    @Test
    void partitionSplitsBySize() {
        List<List<String>> chunks = CollUtils.partition(List.of("a", "b", "c"), 2);

        assertEquals(2, chunks.size());
        assertEquals(List.of("a", "b"), chunks.get(0));
        assertEquals(List.of("c"), chunks.get(1));
    }
}
