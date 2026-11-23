package cn.kennylee.banyan.spring.feign.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * 缺少 Feign 时跳过能力装配并提示。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingClass("feign.RequestInterceptor")
public class FeignMissingClassAutoConfiguration {

    /**
     * 构造时打 WARN。
     */
    public FeignMissingClassAutoConfiguration() {
        log.warn("classpath 缺少 feign.RequestInterceptor，跳过 banyan-spring-feign 能力装配");
    }
}
