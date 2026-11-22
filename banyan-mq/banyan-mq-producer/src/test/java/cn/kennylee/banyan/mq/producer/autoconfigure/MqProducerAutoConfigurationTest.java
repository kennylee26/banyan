package cn.kennylee.banyan.mq.producer.autoconfigure;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import cn.kennylee.banyan.mq.producer.MqProducer;
import cn.kennylee.banyan.mq.spring.MqCommonsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生产者自动装配四种组合测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqProducerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MqCommonsAutoConfiguration.class, MqProducerAutoConfiguration.class))
            .withBean(MqBackend.class, TestBackend::new);

    @Test
    void registersWhenEnabled() {
        // when
        runner.run(context -> assertThat(context).hasSingleBean(MqProducer.class));
    }

    @Test
    void skipsWhenTotalDisabled() {
        // when
        runner.withPropertyValues("banyan.mq.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MqProducer.class);
                    assertThat(context).doesNotHaveBean(MqInstanceRegistry.class);
                });
    }

    @Test
    void skipsWhenProducerDisabled() {
        // when
        runner.withPropertyValues("banyan.mq.producer.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqProducer.class));
    }

    @Test
    void skipsWhenTransactionClassMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader(TransactionSynchronization.class))
                .run(context -> assertThat(context).doesNotHaveBean(MqProducer.class));
    }

    @Test
    void usesCustomProducerBean() {
        // when
        runner.withUserConfiguration(CustomProducerConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MqProducer.class);
                    assertThat(context.getBean(MqProducer.class))
                            .isSameAs(CustomProducerConfig.CUSTOM);
                });
    }

    /**
     * 测试用后端。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class TestBackend implements MqBackend {

        @Override
        public String type() {
            return "rocketmq";
        }

        @Override
        public Set<MqCapability> capabilities() {
            return Set.of(MqCapability.DELAY);
        }

        @Override
        public void send(MqSendRequest request, cn.kennylee.banyan.mq.core.MqMessage message) {
        }

        @Override
        public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        }
    }

    /**
     * 自定义生产者配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomProducerConfig {

        static final MqProducer CUSTOM = new MqProducer(
                new cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry(
                        java.util.List.of(new TestBackend())));

        @Bean
        MqProducer mqProducer() {
            return CUSTOM;
        }
    }
}
