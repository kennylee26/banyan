package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事务提交后发送的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TransactionalSendTest {

    @Test
    void sendsOnlyAfterCommit() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));
        TransactionSynchronizationManager.initSynchronization();
        try {
            // when
            producer.sendAfterCommit(request());

            // then
            assertTrue(backend.sent().isEmpty());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, backend.sent().size());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsImmediatelyWithoutTransaction() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.sendAfterCommit(request());

        // then
        assertEquals(1, backend.sent().size());
    }

    private static MqSendRequest request() {
        return MqSendRequest.builder().instance("rocketmq").topic("orders").payload("x").build();
    }
}
