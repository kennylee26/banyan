package cn.kennylee.banyan.mq.consumer.autoconfigure;

import cn.kennylee.banyan.mq.consumer.MqConsumer;
import cn.kennylee.banyan.mq.consumer.RecordingBackend;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.spring.MqCommonsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消费者自动装配四种组合测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MqCommonsAutoConfiguration.class, MqConsumerAutoConfiguration.class))
            .withBean(MqBackend.class, () -> new RecordingBackend("rocketmq", MqCapability.TAG));

    @Test
    void registersAnnotatedConsumerBean() {
        // when
        runner.withUserConfiguration(ConsumerConfig.class).run(context -> {
            RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
            assertThat(backend.definitions()).hasSize(1);
            assertThat(backend.definitionOf("rocketmq", "orders", "paid")).isNotNull();
        });
    }

    @Test
    void skipsWhenConsumerDisabled() {
        // when
        runner.withUserConfiguration(ConsumerConfig.class)
                .withPropertyValues("banyan.mq.consumer.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MqConsumerAutoConfiguration.class);
                    RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
                    assertThat(backend.definitions()).isEmpty();
                });
    }

    @Test
    void skipsRegistryWhenTotalDisabled() {
        // when
        runner.withUserConfiguration(ConsumerConfig.class)
                .withPropertyValues("banyan.mq.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqInstanceRegistry.class));
    }

    @Test
    void skipsCodecWhenTotalDisabled() {
        // when
        runner.withPropertyValues("banyan.mq.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqMessageCodec.class));
    }

    @Test
    void skipsWhenSmartInitializingSingletonMissing() {
        // when
        runner.withUserConfiguration(ConsumerConfig.class)
                .withClassLoader(new FilteredClassLoader(SmartInitializingSingleton.class))
                .run(context -> assertThat(context).doesNotHaveBean(MqConsumerAutoConfiguration.class));
    }

    @Test
    void usesCustomRegistryBean() {
        // when
        runner.withUserConfiguration(ConsumerConfig.class, CustomRegistryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MqInstanceRegistry.class);
                    RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
                    assertThat(backend.definitions()).hasSize(1);
                });
    }

    @Test
    void detectsConsumerMethodOnlyWhenPresent() {
        // when
        assertTrue(MqConsumerAutoConfiguration.hasConsumerMethod(OrderConsumer.class));

        // then
        assertFalse(MqConsumerAutoConfiguration.hasConsumerMethod(String.class));
    }

    @Test
    void appendsProfileSuffixByDefault() {
        // when
        runner.withUserConfiguration(SuffixedConfig.class)
                .withPropertyValues("spring.profiles.active=uat")
                .run(context -> {
                    RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
                    assertThat(backend.definitionOf("rocketmq", "suffixed", "paid-uat")).isNotNull();
                });
    }

    @Test
    void keepsTagWhenNoProfile() {
        // when
        runner.withUserConfiguration(SuffixedConfig.class)
                .run(context -> {
                    RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
                    assertThat(backend.definitionOf("rocketmq", "suffixed", "paid")).isNotNull();
                });
    }

    @Test
    void suffixDisabledByProperty() {
        // when
        runner.withUserConfiguration(SuffixedConfig.class)
                .withPropertyValues("spring.profiles.active=uat",
                        "banyan.mq.consumer.suffix-enabled=false")
                .run(context -> {
                    RecordingBackend backend = (RecordingBackend) context.getBean(MqBackend.class);
                    assertThat(backend.definitionOf("rocketmq", "suffixed", "paid")).isNotNull();
                });
    }

    /**
     * 业务消费者配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class ConsumerConfig {

        @Bean
        OrderConsumer orderConsumer() {
            return new OrderConsumer();
        }
    }

    /**
     * 自定义注册表配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomRegistryConfig {

        @Bean
        MqInstanceRegistry customRegistry(MqBackend backend) {
            return new MqInstanceRegistry(List.of(backend));
        }
    }

    /**
     * 测试用消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class OrderConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 默认开启后缀的消费者配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class SuffixedConfig {

        @Bean
        SuffixedConsumer suffixedConsumer() {
            return new SuffixedConsumer();
        }
    }

    /**
     * 使用默认后缀开关的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class SuffixedConsumer {

        @MqConsumer(instance = "rocketmq", topic = "suffixed", tag = "paid")
        public void onPaid(MqMessage message) {
        }
    }
}
