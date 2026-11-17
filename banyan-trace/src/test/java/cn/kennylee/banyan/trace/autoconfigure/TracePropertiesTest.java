package cn.kennylee.banyan.trace.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.Ordered;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trace 配置默认值与绑定测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TracePropertiesTest {

    /** 默认值。 */
    @Test
    void hasSensibleDefaults() {
        TraceProperties properties = new TraceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getHeaderName()).isEqualTo("X-Trace-Id");
        assertThat(properties.getMdcKey()).isEqualTo("traceId");
        assertThat(properties.isAcceptIncoming()).isTrue();
        assertThat(properties.isAcceptQueryTraceId()).isFalse();
        assertThat(properties.getResponseHeader().isEnabled()).isTrue();
        assertThat(properties.getServlet().isEnabled()).isTrue();
        assertThat(properties.getWebflux().isEnabled()).isTrue();
        assertThat(properties.getWebflux().getContextPropagation().isEnabled()).isTrue();
        assertThat(properties.getFeign().isEnabled()).isTrue();
        assertThat(properties.getDubbo().isEnabled()).isTrue();
        assertThat(properties.getAsync().isDecorateExecutors()).isTrue();
        assertThat(properties.getServlet().getFilterOrder())
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
    }

    /** 属性可绑定。 */
    @Test
    void bindsCustomValues() {
        Map<String, String> values = Map.ofEntries(
                Map.entry("banyan.trace.enabled", "false"),
                Map.entry("banyan.trace.header-name", "X-Request-Id"),
                Map.entry("banyan.trace.mdc-key", "trace"),
                Map.entry("banyan.trace.accept-incoming", "false"),
                Map.entry("banyan.trace.accept-query-trace-id", "true"),
                Map.entry("banyan.trace.response-header.enabled", "false"),
                Map.entry("banyan.trace.servlet.enabled", "false"),
                Map.entry("banyan.trace.webflux.enabled", "false"),
                Map.entry("banyan.trace.feign.enabled", "false"),
                Map.entry("banyan.trace.dubbo.enabled", "false"),
                Map.entry("banyan.trace.async.decorate-executors", "false"),
                Map.entry("banyan.trace.servlet.filter-order", "5"));

        TraceProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("banyan.trace", Bindable.of(TraceProperties.class))
                .get();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getHeaderName()).isEqualTo("X-Request-Id");
        assertThat(properties.getMdcKey()).isEqualTo("trace");
        assertThat(properties.isAcceptIncoming()).isFalse();
        assertThat(properties.isAcceptQueryTraceId()).isTrue();
        assertThat(properties.getResponseHeader().isEnabled()).isFalse();
        assertThat(properties.getServlet().isEnabled()).isFalse();
        assertThat(properties.getWebflux().isEnabled()).isFalse();
        assertThat(properties.getFeign().isEnabled()).isFalse();
        assertThat(properties.getDubbo().isEnabled()).isFalse();
        assertThat(properties.getAsync().isDecorateExecutors()).isFalse();
        assertThat(properties.getServlet().getFilterOrder()).isEqualTo(5);
    }
}
