package cn.kennylee.banyan.cache.autoconfigure;

import cn.kennylee.banyan.cache.provider.CacheProvider;
import cn.kennylee.banyan.cache.service.CachePenetrationGuard;
import cn.kennylee.banyan.cache.service.CacheService;
import cn.kennylee.banyan.cache.service.NoOpCachePenetrationGuard;
import cn.kennylee.banyan.commons.spring.json.JsonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 缓存自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class CacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JsonAutoConfiguration.class,
                    CacheAutoConfiguration.class,
                    CacheMissingClassAutoConfiguration.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    /** 默认装配：JsonCodec 由 JsonAutoConfiguration 提供，验证自动配置顺序。 */
    @Test
    void loadsByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CacheProperties.class);
            assertThat(context).hasSingleBean(CacheProvider.class);
            assertThat(context).hasSingleBean(CacheService.class);
            assertThat(context).hasSingleBean(NoOpCachePenetrationGuard.class);
        });
    }

    /** 关闭总开关。 */
    @Test
    void skipsWhenDisabled() {
        runner.withPropertyValues("banyan.cache.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheService.class);
                    assertThat(context).doesNotHaveBean(CacheProvider.class);
                });
    }

    /** 缺 Redis 类时跳过并告警。 */
    @Test
    void skipsAndWarnsWhenRedisMissing(CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader(
                        "org.springframework.data.redis.core.StringRedisTemplate"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheService.class);
                });
        assertThat(output).contains("Redis");
    }

    /** 缺 Jackson 时跳过并告警。 */
    @Test
    void skipsAndWarnsWhenJacksonMissing(CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader("com.fasterxml.jackson.databind.ObjectMapper"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheService.class);
                });
        assertThat(output).contains("Jackson");
    }

    /** 自定义 Provider 让位。 */
    @Test
    void backsOffWhenCustomProviderExists() {
        runner.withBean(CacheProvider.class, () -> new NoOpProvider())
                .run(context -> assertThat(context).hasSingleBean(CacheProvider.class));
    }

    /** 自定义 CacheService 让位。 */
    @Test
    void backsOffWhenCustomCacheServiceExists() {
        runner.withBean(CacheService.class, () -> mock(CacheService.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheService.class);
                    assertThat(context).hasSingleBean(CacheProvider.class);
                });
    }

    /** 自定义穿透防护让位。 */
    @Test
    void backsOffWhenCustomGuardExists() {
        runner.withBean(CachePenetrationGuard.class, NoOpCachePenetrationGuard::new)
                .run(context -> assertThat(context).hasSingleBean(CachePenetrationGuard.class));
    }

    /** 缺 Jackson JSR-310 时跳过并告警。 */
    @Test
    void skipsAndWarnsWhenJavaTimeMissing(CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader(
                        "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CacheService.class);
                });
        assertThat(output).contains("JSR-310");
    }

    /** 穿透开关开启但无实现时退回 NoOp 并告警。 */
    @Test
    void warnsWhenPenetrationEnabledWithoutImplementation(CapturedOutput output) {
        runner.withPropertyValues("banyan.cache.penetration.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(NoOpCachePenetrationGuard.class));
        assertThat(output).contains("NoOp");
    }

    /** 测试用空 Provider。 */
    static class NoOpProvider extends cn.kennylee.banyan.cache.provider.AbstractCacheProvider {
    }
}
