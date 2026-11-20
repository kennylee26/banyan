package cn.kennylee.banyan.cache.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存配置默认值与绑定测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class CachePropertiesTest {

    /** 默认值。 */
    @Test
    void hasSensibleDefaults() {
        // given
        CacheProperties properties = new CacheProperties();

        // when
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isReadWriteEnabled()).isTrue();
        assertThat(properties.getMaxKeyLength()).isEqualTo(512);
        assertThat(properties.getTypeMarker().isEnabled()).isTrue();
        assertThat(properties.getPenetration().isEnabled()).isFalse();
        assertThat(properties.getLock().getTtl()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getScan().getBatchSize()).isEqualTo(500);
        assertThat(properties.getScan().getMaxDeletions()).isEqualTo(1000);
    }

    /** 属性可绑定。 */
    @Test
    void bindsCustomValues() {
        // given
        Map<String, String> values = Map.ofEntries(
                Map.entry("banyan.cache.enabled", "false"),
                Map.entry("banyan.cache.read-write-enabled", "false"),
                Map.entry("banyan.cache.max-key-length", "64"),
                Map.entry("banyan.cache.type-marker.enabled", "false"),
                Map.entry("banyan.cache.penetration.enabled", "true"),
                Map.entry("banyan.cache.lock.ttl", "10s"),
                Map.entry("banyan.cache.scan.batch-size", "100"),
                Map.entry("banyan.cache.scan.max-deletions", "200"));

        // when
        CacheProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("banyan.cache", Bindable.of(CacheProperties.class))
                .get();

        // then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isReadWriteEnabled()).isFalse();
        assertThat(properties.getMaxKeyLength()).isEqualTo(64);
        assertThat(properties.getTypeMarker().isEnabled()).isFalse();
        assertThat(properties.getPenetration().isEnabled()).isTrue();
        assertThat(properties.getLock().getTtl()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getScan().getBatchSize()).isEqualTo(100);
        assertThat(properties.getScan().getMaxDeletions()).isEqualTo(200);
    }
}
