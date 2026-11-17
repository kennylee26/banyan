package cn.kennylee.banyan.cache.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 缓存 key 拼接测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class CacheKeysTest {

    /** 标准拼接。 */
    @Test
    void buildsAppNameGroupKey() {
        CacheKeys keys = new CacheKeys("order", 512);

        assertThat(keys.build("cart", "u1")).isEqualTo("order:cart:u1");
    }

    /** 空组名少一个冒号。 */
    @Test
    void omitsEmptyGroupWithoutDoubleColon() {
        CacheKeys keys = new CacheKeys("order", 512);

        assertThat(keys.build(null, "u1")).isEqualTo("order:u1");
        assertThat(keys.build("", "u1")).isEqualTo("order:u1");
        assertThat(keys.build("  ", "u1")).isEqualTo("order:u1");
    }

    /** 没有 appName 时退化。 */
    @Test
    void degradesWhenAppNameMissing() {
        CacheKeys keys = new CacheKeys(null, 512);

        assertThat(keys.build("cart", "u1")).isEqualTo("cart:u1");
        assertThat(keys.build("", "u1")).isEqualTo("u1");
    }

    /** 组名或 key 含分隔符时失败。 */
    @Test
    void rejectsSeparatorInGroupOrKey() {
        CacheKeys keys = new CacheKeys("order", 512);

        assertThatThrownBy(() -> keys.build("car:t", "u1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> keys.build("cart", "u:1")).isInstanceOf(IllegalArgumentException.class);
    }

    /** 空 key 失败。 */
    @Test
    void rejectsBlankKey() {
        CacheKeys keys = new CacheKeys("order", 512);

        assertThatThrownBy(() -> keys.build("cart", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> keys.build("cart", null)).isInstanceOf(IllegalArgumentException.class);
    }

    /** 超长 key 失败。 */
    @Test
    void rejectsTooLongKey() {
        CacheKeys keys = new CacheKeys("order", 8);

        assertThatThrownBy(() -> keys.build("cart", "very-long-key")).isInstanceOf(IllegalArgumentException.class);
    }

    /** 命名空间前缀用于批量删除。 */
    @Test
    void buildsNamespacePrefix() {
        CacheKeys keys = new CacheKeys("order", 512);

        assertThat(keys.namespace("cart")).isEqualTo("order:cart:");
        assertThat(keys.namespace("")).isEqualTo("order:");
    }
}
