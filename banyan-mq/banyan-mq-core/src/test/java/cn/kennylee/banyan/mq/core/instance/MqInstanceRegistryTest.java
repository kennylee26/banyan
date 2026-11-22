package cn.kennylee.banyan.mq.core.instance;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逻辑实例路由与能力校验的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqInstanceRegistryTest {

    @Test
    void defaultsInstanceNameToBackendType() {
        // given
        FakeBackend kafka = new FakeBackend("kafka", Set.of(MqCapability.TAG));

        // when
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(kafka));

        // then
        assertSame(kafka, registry.backendOf("kafka"));
    }

    @Test
    void routesToDeclaredInstance() {
        // given
        FakeBackend kafka = new FakeBackend("kafka", Set.of());
        FakeBackend rocketmq = new FakeBackend("rocketmq", Set.of(MqCapability.TAG));

        // when
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(kafka, rocketmq),
                Map.of("orders", "kafka", "notify", "rocketmq"));

        // then
        assertSame(kafka, registry.backendOf("orders"));
        assertSame(rocketmq, registry.backendOf("notify"));
    }

    @Test
    void rejectsUndeclaredInstance() {
        // given
        MqInstanceRegistry registry = new MqInstanceRegistry(
                List.of(new FakeBackend("kafka", Set.of())));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registry.backendOf("missing"));
        assertTrue(error.getMessage().contains("missing"));
    }

    @Test
    void rejectsUnsupportedCapability() {
        // given
        MqInstanceRegistry registry = new MqInstanceRegistry(
                List.of(new FakeBackend("kafka", Set.of())));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registry.requireCapability("kafka", MqCapability.TAG));
        assertTrue(error.getMessage().contains("kafka"));
        assertTrue(error.getMessage().contains("TAG"));
    }

    @Test
    void acceptsSupportedCapability() {
        // given
        MqInstanceRegistry registry = new MqInstanceRegistry(
                List.of(new FakeBackend("rocketmq", Set.of(MqCapability.TAG))));

        // when
        assertEquals(Set.of(MqCapability.TAG),
                registry.backendOf("rocketmq").capabilities());
    }

    @Test
    void allowsSameTypeMultipleInstances() {
        // given
        FakeBackend memberKafka = new FakeBackend("kafka", Set.of());
        FakeBackend paymentKafka = new FakeBackend("kafka", Set.of());

        // when
        MqInstanceRegistry registry = new MqInstanceRegistry(Map.of(
                "member-kafka", memberKafka,
                "payment-kafka", paymentKafka));

        // then
        assertSame(memberKafka, registry.backendOf("member-kafka"));
        assertSame(paymentKafka, registry.backendOf("payment-kafka"));
    }

    @Test
    void rejectsDuplicateBackendType() {
        // given
        FakeBackend first = new FakeBackend("kafka", Set.of());
        FakeBackend second = new FakeBackend("kafka", Set.of());

        // when
        assertThrows(MqConfigurationException.class,
                () -> new MqInstanceRegistry(List.of(first, second)));
    }

    /**
     * 测试用后端。
     *
     * @author kennylee
     * @since 0.1.0
     */
    private static final class FakeBackend implements MqBackend {

        private final String type;
        private final Set<MqCapability> capabilities;

        private FakeBackend(String type, Set<MqCapability> capabilities) {
            this.type = type;
            this.capabilities = capabilities;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public Set<MqCapability> capabilities() {
            return capabilities;
        }

        @Override
        public void send(MqSendRequest request, cn.kennylee.banyan.mq.core.MqMessage message) {
        }

        @Override
        public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        }
    }
}
