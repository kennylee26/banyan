package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 记录消费者定义的假后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class RecordingBackend implements MqBackend {

    private final String type;
    private final Set<MqCapability> capabilities;
    private final Map<String, MqConsumerDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, MqConsumerHandler> handlers = new LinkedHashMap<>();

    public RecordingBackend(String type, MqCapability... capabilities) {
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
    public void send(MqSendRequest request, cn.kennylee.banyan.mq.core.MqMessage message) {
    }

    @Override
    public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        String key = definition.getInstance() + ":" + definition.getTopic() + ":" + definition.getTag();
        definitions.put(key, definition);
        handlers.put(key, handler);
    }

    public Map<String, MqConsumerDefinition> definitions() {
        return definitions;
    }

    public MqConsumerHandler handlerOf(String instance, String topic, String tag) {
        return handlers.get(instance + ":" + topic + ":" + tag);
    }

    public MqConsumerDefinition definitionOf(String instance, String topic, String tag) {
        return definitions.get(instance + ":" + topic + ":" + tag);
    }
}
