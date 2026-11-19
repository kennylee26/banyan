package cn.kennylee.banyan.log.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志配置默认值与绑定测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LogPropertiesTest {

    /** 默认值。 */
    @Test
    void hasSensibleDefaults() {
        LogProperties properties = new LogProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getLifecycle().isEnabled()).isTrue();
        assertThat(properties.getLifecycle().isEnvironmentEnabled()).isTrue();
        assertThat(properties.getLifecycle().getBanner()).isNull();
    }

    /** 属性可绑定。 */
    @Test
    void bindsCustomValues() {
        Map<String, String> values = Map.of(
                "banyan.log.enabled", "false",
                "banyan.log.lifecycle.enabled", "false",
                "banyan.log.lifecycle.environment-enabled", "false",
                "banyan.log.lifecycle.banner", "custom {0} banner");

        LogProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("banyan.log", Bindable.of(LogProperties.class))
                .get();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getLifecycle().isEnabled()).isFalse();
        assertThat(properties.getLifecycle().isEnvironmentEnabled()).isFalse();
        assertThat(properties.getLifecycle().getBanner()).isEqualTo("custom {0} banner");
    }
}
