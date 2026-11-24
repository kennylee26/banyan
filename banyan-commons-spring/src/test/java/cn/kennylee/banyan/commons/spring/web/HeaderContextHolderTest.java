package cn.kennylee.banyan.commons.spring.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单次 header 上下文测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class HeaderContextHolderTest {

    @AfterEach
    void clear() {
        HeaderContextHolder.clear();
        HeaderContextHolder.setGloballyEnabled(true);
    }

    /** 默认允许转发并自动清理。 */
    @Test
    void defaultsToForwardAndClear() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContext context = HeaderContextHolder.current();

        // then
        assertTrue(context.isForwardHeaders());
        assertTrue(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().isEmpty());
    }

    /** 单次控制：阻塞头、自定义头、禁用转发、关闭自动清理。 */
    @Test
    void supportsPerCallControls() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContextHolder.addBlockedHeaders(List.of("Authorization"));
        HeaderContextHolder.addCustomHeader("X-Tenant", "t1");
        HeaderContextHolder.disableForwardHeaders();
        HeaderContextHolder.setClearAtOnce(false);

        // then
        HeaderContext context = HeaderContextHolder.current();
        assertFalse(context.isForwardHeaders());
        assertFalse(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().contains("Authorization"));
        assertEquals(Map.of("X-Tenant", "t1"), context.getCustomHeaders());
    }

    /** 全局关闭后，即使单次开启也不转发。 */
    @Test
    void globalSwitchDisablesAllForwarding() {
        // given
        HeaderContextHolder.setGloballyEnabled(false);

        // when
        boolean enabled = HeaderContextHolder.isForwardEnabled();

        // then
        assertFalse(enabled);
    }

    /** 自定义头可整体替换，清理后回到空表。 */
    @Test
    void replacesCustomHeadersAndClears() {
        // given
        HeaderContextHolder.addCustomHeader("X-Old", "1");

        // when
        HeaderContextHolder.setCustomHeaders(Map.of("X-New", "2"));

        // then
        assertEquals(Map.of("X-New", "2"), HeaderContextHolder.current().getCustomHeaders());

        // and
        HeaderContextHolder.clear();
        assertTrue(HeaderContextHolder.current().getCustomHeaders().isEmpty());
    }
}
