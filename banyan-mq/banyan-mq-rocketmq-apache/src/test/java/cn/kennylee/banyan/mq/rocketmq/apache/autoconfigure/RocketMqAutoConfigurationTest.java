package cn.kennylee.banyan.mq.rocketmq.apache.autoconfigure;

import cn.kennylee.banyan.mq.core.spi.MqBackend;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RocketMQ 后端自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RocketMqAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RocketMqAutoConfiguration.class))
            .withBean(RocketMQTemplate.class, () -> mock(RocketMQTemplate.class));

    @Test
    void registersWhenClassPresent() {
        // when
        runner.run(context -> assertThat(context).hasSingleBean(MqBackend.class));
    }

    @Test
    void skipsWhenDisabled() {
        // when
        runner.withPropertyValues("banyan.mq.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqBackend.class));
    }

    @Test
    void skipsWhenModuleDisabled() {
        // when
        runner.withPropertyValues("banyan.mq.rocketmq.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqBackend.class));
    }

    @Test
    void skipsWhenRocketMqTemplateMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader(RocketMQTemplate.class))
                .run(context -> assertThat(context).doesNotHaveBean(MqBackend.class));
    }

    @Test
    void usesCustomBackendBean() {
        // when
        runner.withUserConfiguration(CustomBackendConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MqBackend.class);
                    assertThat(context.getBean(MqBackend.class))
                            .isSameAs(CustomBackendConfig.CUSTOM);
                });
    }

    /**
     * 自定义后端配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomBackendConfig {

        static final cn.kennylee.banyan.mq.rocketmq.apache.RocketMqBackend CUSTOM =
                new cn.kennylee.banyan.mq.rocketmq.apache.RocketMqBackend(
                        mock(RocketMQTemplate.class), "127.0.0.1:9876",
                        mock(org.springframework.context.ApplicationContext.class));

        @org.springframework.context.annotation.Bean
        cn.kennylee.banyan.mq.rocketmq.apache.RocketMqBackend customBackend() {
            return CUSTOM;
        }
    }
}
