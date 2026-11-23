package cn.kennylee.banyan.spring.feign.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Feign 能力配置。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.spring.feign")
public class FeignProperties {

    /** 总开关。 */
    private boolean enabled = true;

    /** header 转发配置。 */
    private ForwardHeaders forwardHeaders = new ForwardHeaders();

    /** 请求日志配置。 */
    private Toggle requestLog = new Toggle();

    /** 响应日志配置。 */
    private Toggle responseLog = new Toggle();

    /** 日志细节配置。 */
    private Logging logging = new Logging();

    /**
     * 简单的能力开关。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Toggle {

        /** 是否启用。 */
        private boolean enabled = true;
    }

    /**
     * header 转发配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class ForwardHeaders {

        /** 默认关闭，避免自动带出敏感入站 header。 */
        private boolean enabled;

        /** apply 后自动清理上下文。 */
        private boolean clearAfterApply = true;

        /** 额外阻塞头。 */
        private List<String> blockedHeaders = new ArrayList<>();
    }

    /**
     * 日志配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Logging {

        /** 默认 body 打印上限。 */
        public static final int DEFAULT_MAX_BODY_LENGTH = 2048;

        /** 敏感值脱敏。 */
        private boolean maskSensitive = true;

        /** 需要脱敏的 header。 */
        private List<String> maskedHeaders = new ArrayList<>(
                List.of("Authorization", "Cookie", "Set-Cookie"));

        /** 完全不打印的 header。 */
        private List<String> ignoredHeaders = new ArrayList<>();

        /** body 打印上限。 */
        private int maxBodyLength = DEFAULT_MAX_BODY_LENGTH;
    }
}
