package cn.kennylee.banyan.cache.provider;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认适配器未实现方法的降级测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class AbstractCacheProviderTest {

    private final CacheProvider provider = new AbstractCacheProvider() {
    };

    /** 未实现的方法抛出明确异常。 */
    @Test
    void throwsUnsupportedForUnimplementedMethods() {
        // when
        assertThatThrownBy(() -> provider.get("k")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.set("k", "v", Duration.ofSeconds(1)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.hashPut("k", "f", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.execute("return 1", java.util.List.of("k")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
