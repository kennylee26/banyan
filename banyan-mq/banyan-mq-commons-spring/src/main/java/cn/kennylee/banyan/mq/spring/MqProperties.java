package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.commons.util.Strings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息统一配置。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.mq")
public class MqProperties {

    /** 装配总开关，默认 true。 */
    private boolean enabled = true;

    /** 环境后缀，为空时使用首个激活 profile。 */
    private String suffix;

    /** 生产者配置。 */
    private Producer producer = new Producer();

    /** 消费者配置。 */
    private Consumer consumer = new Consumer();

    /** 逻辑实例：实例名到后端类型的映射。 */
    private Map<String, Instance> instances = new LinkedHashMap<>();

    /**
     * 生效的环境后缀：显式配置优先，否则取首个激活 profile。
     *
     * @param activeProfiles 激活的 profile
     * @return 环境后缀，空字符串表示不追加
     */
    public String effectiveSuffix(String[] activeProfiles) {
        if (Strings.isNotBlank(suffix)) {
            return suffix;
        }
        return activeProfiles.length == 0 ? "" : activeProfiles[0];
    }

    /**
     * 生产者配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Producer {

        /** 生产者模块开关，默认 true。 */
        private boolean enabled = true;

        /** 是否默认给 tag 追加环境后缀，默认 true。 */
        private boolean tagSuffixEnabled = true;
    }

    /**
     * 消费者配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Consumer {

        /** 消费者模块开关，默认 true。 */
        private boolean enabled = true;

        /** 是否给消费组与 tag 追加环境后缀，默认 true。 */
        private boolean suffixEnabled = true;
    }

    /**
     * 逻辑实例配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Instance {

        /** 后端类型标识，例如 kafka、rocketmq。 */
        private String type;
    }
}
