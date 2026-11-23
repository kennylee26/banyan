package cn.kennylee.banyan.spring.feign;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Feign 单次上下文测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class FeignContextHolderTest {

    @AfterEach
    void clear() {
        FeignContextHolder.clear();
        FeignContextHolder.setGloballyEnabled(true);
    }

    /** 默认允许转发并自动清理。 */
    @Test
    void defaultsToForwardAndClear() {
        // given
        FeignContextHolder.clear();

        // when
        FeignContext context = FeignContextHolder.current();

        // then
        assertTrue(context.isForwardHeaders());
        assertTrue(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().isEmpty());
    }

    /** 单次控制：阻塞头、自定义头、禁用转发、关闭自动清理。 */
    @Test
    void supportsPerCallControls() {
        // given
        FeignContextHolder.clear();

        // when
        FeignContextHolder.addBlockedHeaders(List.of("Authorization"));
        FeignContextHolder.addCustomHeader("X-Tenant", "t1");
        FeignContextHolder.disableForwardHeaders();
        FeignContextHolder.setClearAtOnce(false);

        // then
        FeignContext context = FeignContextHolder.current();
        assertFalse(context.isForwardHeaders());
        assertFalse(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().contains("Authorization"));
        assertEquals(Map.of("X-Tenant", "t1"), context.getCustomHeaders());
    }

    /** 全局关闭后，即使单次开启也不转发。 */
    @Test
    void globalSwitchDisablesAllForwarding() {
        // given
        FeignContextHolder.setGloballyEnabled(false);

        // when
        boolean enabled = FeignContextHolder.isForwardEnabled();

        // then
        assertFalse(enabled);
    }
}
