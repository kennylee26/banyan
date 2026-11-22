package cn.kennylee.banyan.mq.core.instance;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.spi.MqBackend;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 逻辑实例路由：实例名解析到后端，并在启动期校验实例声明与能力支持。
 *
 * <p>支持两种声明方式：按后端类型自动生成实例（实例名 = 类型），
 * 或显式给出「实例名到后端实现」的映射，从而允许同类型多实例（例如多个 Kafka 集群）。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class MqInstanceRegistry {

    /** 实例名到后端实现。 */
    private final Map<String, MqBackend> instanceToBackend;

    /**
     * 按后端类型建立默认实例。
     *
     * @param backends 后端实现集合
     * @throws MqConfigurationException 后端类型重复
     */
    public MqInstanceRegistry(Collection<MqBackend> backends) {
        this(backends, Map.of());
    }

    /**
     * 按后端类型建立实例，并补充显式的实例名映射。
     *
     * @param backends       后端实现集合
     * @param instanceToType 实例名到后端类型的映射
     * @throws MqConfigurationException 后端类型重复或实例引用未声明的后端
     */
    public MqInstanceRegistry(Collection<MqBackend> backends, Map<String, String> instanceToType) {
        Map<String, MqBackend> byType = new LinkedHashMap<>();
        for (MqBackend backend : backends) {
            if (byType.putIfAbsent(backend.type(), backend) != null) {
                throw new MqConfigurationException(Messages.get(
                        ErrorCode.MQ_CONFIGURATION_INVALID, "重复的后端类型: " + backend.type()));
            }
        }

        Map<String, MqBackend> mapping = new LinkedHashMap<>();
        if (instanceToType.isEmpty()) {
            for (MqBackend backend : backends) {
                mapping.put(backend.type(), backend);
            }
        } else {
            instanceToType.forEach((name, type) -> {
                MqBackend backend = byType.get(type);
                if (backend == null) {
                    throw new MqConfigurationException(Messages.get(
                            ErrorCode.MQ_CONFIGURATION_INVALID,
                            "逻辑实例 " + name + " 引用了未声明的后端类型: " + type));
                }
                mapping.put(name, backend);
            });
        }
        this.instanceToBackend = Collections.unmodifiableMap(mapping);
    }

    /**
     * 显式给出实例名到后端实现的映射，允许同类型多实例。
     *
     * @param instanceToBackend 实例名到后端实现
     * @throws MqConfigurationException 实例为空
     */
    public MqInstanceRegistry(Map<String, MqBackend> instanceToBackend) {
        if (instanceToBackend == null || instanceToBackend.isEmpty()) {
            throw new MqConfigurationException(Messages.get(
                    ErrorCode.MQ_CONFIGURATION_INVALID, "至少需要声明一个逻辑实例"));
        }
        this.instanceToBackend = Collections.unmodifiableMap(new LinkedHashMap<>(instanceToBackend));
    }

    /**
     * 全部逻辑实例名。
     *
     * @return 实例名集合
     */
    public Set<String> instanceNames() {
        return Set.copyOf(instanceToBackend.keySet());
    }

    /**
     * 取默认实例：仅在只声明了一个实例时可用。
     *
     * @return 默认实例名
     * @throws MqConfigurationException 实例不唯一
     */
    public String defaultInstance() {
        if (instanceToBackend.size() == 1) {
            return instanceToBackend.keySet().iterator().next();
        }
        throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                "未指定逻辑实例且候选实例不唯一: " + instanceToBackend.keySet()));
    }

    /**
     * 取实例对应的后端。
     *
     * @param instanceName 逻辑实例名
     * @return 后端实现
     * @throws MqConfigurationException 实例未声明
     */
    public MqBackend backendOf(String instanceName) {
        MqBackend backend = instanceToBackend.get(instanceName);
        if (backend == null) {
            throw new MqConfigurationException(Messages.get(
                    ErrorCode.MQ_CONFIGURATION_INVALID, "未声明的逻辑实例: " + instanceName));
        }
        return backend;
    }

    /**
     * 校验实例后端是否支持某能力。
     *
     * @param instanceName 逻辑实例名
     * @param capability   能力
     * @throws MqConfigurationException 后端不支持该能力
     */
    public void requireCapability(String instanceName, MqCapability capability) {
        backendOf(instanceName).requireCapability(capability);
    }
}
