package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqConstants;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.StringMqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 消费者注册：扫描业务类的注解方法，转发给对应的逻辑实例后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class MqConsumerRegistrar {

    /** 逻辑实例路由。 */
    private final MqInstanceRegistry registry;

    /** 环境后缀，为空表示不追加。 */
    private final String suffix;

    /** 编解码器。 */
    private final MqMessageCodec codec;

    /** 环境配置，用于解析注解属性中的占位符。 */
    private final Environment environment;

    /**
     * 不追加环境后缀的构造器。
     *
     * @param registry 逻辑实例路由
     */
    public MqConsumerRegistrar(MqInstanceRegistry registry) {
        this(registry, "");
    }

    /**
     * 使用默认编解码构造注册器。
     *
     * @param registry 逻辑实例路由
     * @param suffix   环境后缀，可为空
     */
    public MqConsumerRegistrar(MqInstanceRegistry registry, String suffix) {
        this(registry, suffix, new StringMqMessageCodec());
    }

    /**
     * 构造注册器。
     *
     * @param registry 逻辑实例路由
     * @param suffix   环境后缀，可为空
     * @param codec    编解码器
     */
    public MqConsumerRegistrar(MqInstanceRegistry registry, String suffix, MqMessageCodec codec) {
        this(registry, suffix, codec, null);
    }

    /**
     * 构造注册器。
     *
     * @param registry    逻辑实例路由
     * @param suffix      环境后缀，可为空
     * @param codec       编解码器
     * @param environment 环境配置，可为空
     */
    public MqConsumerRegistrar(MqInstanceRegistry registry, String suffix, MqMessageCodec codec,
                               Environment environment) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.suffix = suffix == null ? "" : suffix;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.environment = environment;
    }

    /**
     * 注册一个业务 bean 上的全部消费者方法。
     *
     * @param bean 业务 bean
     * @throws MqConfigurationException 消费者方法签名不受支持或使用了后端不支持的能力
     */
    public void register(Object bean) {
        Objects.requireNonNull(bean, "bean");
        Class<?> userClass = ClassUtils.getUserClass(bean);
        for (Method method : userClass.getDeclaredMethods()) {
            MqConsumer annotation = method.getAnnotation(MqConsumer.class);
            if (annotation != null) {
                registerMethod(bean, method, annotation);
            }
        }
    }

    private void registerMethod(Object bean, Method method, MqConsumer annotation) {
        String instance = resolveInstance(resolvePlaceholders(annotation.instance()));
        List<String> tags = resolveTags(annotation.tag(), annotation.tagSuffixEnabled());
        boolean wildcard = tags.isEmpty();
        String tag = wildcard ? MqConstants.WILDCARD_TAG : String.join(
                " " + MqConstants.TAG_SEPARATOR + " ", tags);
        if (!wildcard) {
            registry.requireCapability(instance,
                    tags.size() > 1 ? MqCapability.MULTI_TAG : MqCapability.TAG);
        }

        MqConsumerDefinition definition = MqConsumerDefinition.builder()
                .instance(instance)
                .topic(resolvePlaceholders(annotation.topic()))
                .tag(tag)
                .group(resolveGroup(bean, method, annotation))
                .concurrency(annotation.concurrency())
                .maxRetryTimes(annotation.maxRetryTimes())
                .build();

        log.info(Messages.get(ErrorCode.MQ_CONSUMER_REGISTERED,
                instance, definition.getTopic(), definition.getTag(), definition.getGroup(),
                definition.getConcurrency(), definition.getMaxRetryTimes()));
        registry.backendOf(instance).registerConsumer(definition, handlerOf(bean, method));
    }

    private String resolveInstance(String configured) {
        return Strings.isBlank(configured) ? registry.defaultInstance() : configured;
    }

    private String resolveGroup(Object bean, Method method, MqConsumer annotation) {
        String group = resolvePlaceholders(annotation.group());
        if (Strings.isBlank(group)) {
            group = ClassUtils.getUserClass(bean).getSimpleName() + "_" + method.getName() + "Consumer";
        }
        return applySuffix(group, annotation.groupSuffixEnabled());
    }

    /**
     * 把注解的 tag 数组解析成去空、去重、逐 tag 追加后缀的列表。
     *
     * <p>空数组或只有 {@code "*"} 时返回空列表，表示不过滤；{@code "*"} 不能与其它 tag 混用。
     * 用户只写数组，分隔符由框架内部拼接，数组元素里出现分隔符会直接报错。</p>
     *
     * @param values        注解上的 tag 数组
     * @param suffixEnabled 是否追加环境后缀
     * @return 解析后的 tag 列表；空列表表示通配
     * @throws MqConfigurationException tag 含分隔符或与通配符混用
     */
    private List<String> resolveTags(String[] values, boolean suffixEnabled) {
        List<String> tags = new ArrayList<>();
        for (String value : values) {
            if (Strings.isBlank(value)) {
                continue;
            }
            String resolved = resolvePlaceholders(value);
            if (resolved.contains(MqConstants.TAG_SEPARATOR)) {
                throw new MqConfigurationException(Messages.get(
                        ErrorCode.MQ_TAG_SEPARATOR_NOT_ALLOWED, resolved));
            }
            tags.add(MqConstants.WILDCARD_TAG.equals(resolved)
                    ? resolved : applySuffix(resolved, suffixEnabled));
        }
        List<String> distinct = tags.stream().distinct().toList();
        if (distinct.contains(MqConstants.WILDCARD_TAG)) {
            if (distinct.size() > 1) {
                throw new MqConfigurationException(Messages.get(
                        ErrorCode.MQ_TAG_WILDCARD_MIXED, distinct));
            }
            return List.of();
        }
        return distinct;
    }

    private String applySuffix(String value, boolean enabled) {
        if (!enabled || suffix.isBlank()) {
            return value;
        }
        return value + "-" + suffix;
    }

    private String resolvePlaceholders(String value) {
        return environment == null ? value : environment.resolvePlaceholders(value);
    }

    private MqConsumerHandler handlerOf(Object bean, Method method) {
        validateSignature(method);
        method.setAccessible(true);
        boolean booleanReturn = method.getReturnType() == boolean.class
                || method.getReturnType() == Boolean.class;
        int parameterCount = method.getParameterCount();
        Class<?> firstParameterType = method.getParameterTypes()[0];
        boolean rawMessage = MqMessage.class.isAssignableFrom(firstParameterType);

        return (message, context) -> {
            MqMessage enriched = message.withCodec(codec);
            Object firstArgument = rawMessage
                    ? enriched
                    : codec.decode(message.getBody(), firstParameterType);
            Object result;
            try {
                result = parameterCount == 1
                        ? method.invoke(bean, firstArgument)
                        : method.invoke(bean, firstArgument, context);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(Messages.get(ErrorCode.MQ_CONSUME_FAILED,
                        method.getName()), cause);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(Messages.get(ErrorCode.MQ_CONSUME_FAILED,
                        method.getName()), e);
            }
            return booleanReturn ? (Boolean) result : true;
        };
    }

    private void validateSignature(Method method) {
        Class<?> returnType = method.getReturnType();
        boolean supportedReturn = returnType == void.class || returnType == boolean.class
                || returnType == Boolean.class;
        if (!supportedReturn) {
            throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                    "消费者方法只支持 void 或 boolean 返回值: " + method.getName()));
        }
        int parameterCount = method.getParameterCount();
        if (parameterCount != 1 && parameterCount != 2) {
            throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                    "消费者方法只支持 1 或 2 个参数: " + method.getName()));
        }
    }
}
