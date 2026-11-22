package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯 JDK 假后端：记录发送请求与消息体、保存消费者处理器，不建立任何网络连接。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class FakeMqBackend implements MqBackend {

    private final String type;
    private final Set<MqCapability> capabilities;
    private final List<MqSendRequest> sent = new ArrayList<>();
    private final List<MqMessage> messages = new ArrayList<>();
    private final Map<String, MqConsumerHandler> handlers = new LinkedHashMap<>();

    FakeMqBackend(String type, MqCapability... capabilities) {
        this.type = type;
        this.capabilities = Set.of(capabilities);
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
    public void send(MqSendRequest request, MqMessage message) {
        sent.add(request);
        messages.add(message);
    }

    @Override
    public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        handlers.put(key(definition), handler);
    }

    List<MqSendRequest> sent() {
        return sent;
    }

    List<MqMessage> messages() {
        return messages;
    }

    int consumerCount() {
        return handlers.size();
    }

    boolean deliver(String instance, String topic, String tag, String body) {
        MqConsumerHandler handler = handlers.get(key(instance, topic, tag));
        if (handler == null) {
            return false;
        }
        MqMessage message = MqMessage.builder().body(body).build();
        MqConsumeContext context = MqConsumeContext.builder()
                .instance(instance).topic(topic).tag(tag).build();
        return handler.handle(message, context);
    }

    private static String key(MqConsumerDefinition definition) {
        return key(definition.getInstance(), definition.getTopic(), definition.getTag());
    }

    private static String key(String instance, String topic, String tag) {
        return instance + ":" + topic + ":" + tag;
    }
}
