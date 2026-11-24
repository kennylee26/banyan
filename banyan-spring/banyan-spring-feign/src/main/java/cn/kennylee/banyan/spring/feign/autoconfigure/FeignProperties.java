package cn.kennylee.banyan.spring.feign.autoconfigure;

import cn.kennylee.banyan.commons.spring.log.LoggingOptions;
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
    private LoggingOptions logging = new LoggingOptions();

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
}
