package cn.kennylee.banyan.commons.http;

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

    /** 默认允许传播并自动清理。 */
    @Test
    void defaultsToPropagateAndClear() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContext context = HeaderContextHolder.current();

        // then
        assertTrue(context.isPropagateHeaders());
        assertTrue(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().isEmpty());
    }

    /** 单次控制：阻塞头、自定义头、禁用传播、关闭自动清理。 */
    @Test
    void supportsPerCallControls() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContextHolder.addBlockedHeaders(List.of("Authorization"));
        HeaderContextHolder.addCustomHeader("X-Tenant", "t1");
        HeaderContextHolder.disablePropagation();
        HeaderContextHolder.setClearAtOnce(false);

        // then
        HeaderContext context = HeaderContextHolder.current();
        assertFalse(context.isPropagateHeaders());
        assertFalse(context.isClearAtOnce());
        assertTrue(context.getBlockedHeaders().contains("Authorization"));
        assertEquals(Map.of("X-Tenant", "t1"), context.getCustomHeaders());
    }

    /** 全局关闭后，即使单次开启也不传播。 */
    @Test
    void globalSwitchDisablesAllPropagation() {
        // given
        HeaderContextHolder.setGloballyEnabled(false);

        // when
        boolean enabled = HeaderContextHolder.isPropagationEnabled();

        // then
        assertFalse(enabled);
    }

    /** 全局与单次都开启时允许传播。 */
    @Test
    void propagationEnabledWhenGlobalAndPerCallAllow() {
        // given
        HeaderContextHolder.setGloballyEnabled(true);
        HeaderContextHolder.clear();

        // when
        HeaderContextHolder.enablePropagation();

        // then
        assertTrue(HeaderContextHolder.isPropagationEnabled());
    }

    /** 追加与整体替换自定义头。 */
    @Test
    void addsAndReplacesCustomHeaders() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContextHolder.addCustomHeaders(Map.of("X-A", "1", "X-B", "2"));

        // then
        assertEquals(Map.of("X-A", "1", "X-B", "2"),
                HeaderContextHolder.current().getCustomHeaders());

        // when
        HeaderContextHolder.setCustomHeaders(Map.of("X-New", "3"));

        // then
        assertEquals(Map.of("X-New", "3"), HeaderContextHolder.current().getCustomHeaders());
    }

    /** 追加单个阻塞头，清理后上下文回到默认值。 */
    @Test
    void addsSingleBlockedHeaderAndClears() {
        // given
        HeaderContextHolder.clear();

        // when
        HeaderContextHolder.addBlockedHeader("X-Internal");

        // then
        assertTrue(HeaderContextHolder.current().getBlockedHeaders().contains("X-Internal"));

        // when
        HeaderContextHolder.clear();

        // then
        assertTrue(HeaderContextHolder.current().getBlockedHeaders().isEmpty());
    }
}
