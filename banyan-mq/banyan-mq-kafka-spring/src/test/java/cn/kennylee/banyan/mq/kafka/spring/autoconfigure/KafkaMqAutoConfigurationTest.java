package cn.kennylee.banyan.mq.kafka.spring.autoconfigure;

import cn.kennylee.banyan.mq.core.spi.MqBackend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Kafka 后端自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class KafkaMqAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaMqAutoConfiguration.class))
            .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
            .withBean(ConsumerFactory.class, () -> mock(ConsumerFactory.class));

    @Test
    void registersWhenClassesPresent() {
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
        runner.withPropertyValues("banyan.mq.kafka.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqBackend.class));
    }

    @Test
    void skipsWhenKafkaTemplateMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader(KafkaTemplate.class))
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

        static final cn.kennylee.banyan.mq.kafka.spring.KafkaMqBackend CUSTOM =
                new cn.kennylee.banyan.mq.kafka.spring.KafkaMqBackend(
                        mock(KafkaTemplate.class), mock(ConsumerFactory.class));

        @org.springframework.context.annotation.Bean
        cn.kennylee.banyan.mq.kafka.spring.KafkaMqBackend customBackend() {
            return CUSTOM;
        }
    }
}
