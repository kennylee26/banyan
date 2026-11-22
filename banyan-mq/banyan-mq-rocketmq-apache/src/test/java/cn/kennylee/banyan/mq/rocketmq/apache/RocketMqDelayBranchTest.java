package cn.kennylee.banyan.mq.rocketmq.apache;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * RocketMQ 延迟消息按客户端版本分支的测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RocketMqDelayBranchTest {

    @Test
    void usesDelayLevelBeforeVersionFive() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqDelayStrategy strategy = new RocketMqDelayStrategy(template, 4);
        Message<String> message = MessageBuilder.withPayload("x").build();

        // when
        strategy.send("orders", message, Duration.ofSeconds(70));

        // then
        verify(template).syncSend(eq("orders"), any(Message.class), anyLong(), eq(5));
        verifyNoMoreInteractions(template);
    }

    @Test
    void usesArbitraryTimeFromVersionFive() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqDelayStrategy strategy = new RocketMqDelayStrategy(template, 5);
        Message<String> message = MessageBuilder.withPayload("x").build();

        // when
        strategy.send("orders", message, Duration.ofSeconds(70));

        // then
        verify(template).syncSendDelayTimeMills(eq("orders"), any(Message.class), eq(70000L));
        verifyNoMoreInteractions(template);
    }

    @Test
    void picksClosestDelayLevel() {
        // when
        assertEquals(1, RocketMqDelayStrategy.closestLevel(Duration.ofSeconds(1)));

        // then
        assertEquals(5, RocketMqDelayStrategy.closestLevel(Duration.ofSeconds(60)));
        assertEquals(5, RocketMqDelayStrategy.closestLevel(Duration.ofSeconds(70)));
        assertEquals(18, RocketMqDelayStrategy.closestLevel(Duration.ofHours(2)));
        assertEquals(18, RocketMqDelayStrategy.closestLevel(Duration.ofHours(5)));
    }

    @Test
    void prefersEarlierLevelOnTie() {
        // when
        assertEquals(1, RocketMqDelayStrategy.closestLevel(Duration.ofSeconds(3)));
    }

    @Test
    void parsesMajorVersionFromDescription() {
        // when
        assertEquals(4, RocketMqDelayStrategy.parseMajorVersion("V4_9_8"));

        // then
        assertEquals(4, RocketMqDelayStrategy.parseMajorVersion("4.9.8"));
        assertEquals(5, RocketMqDelayStrategy.parseMajorVersion("V5_1_4"));
    }

    @Test
    void fallsBackToFourWhenNoDigit() {
        // when
        assertEquals(4, RocketMqDelayStrategy.parseMajorVersion("unknown"));

        // then
        assertEquals(4, RocketMqDelayStrategy.parseMajorVersion(null));
    }
}
