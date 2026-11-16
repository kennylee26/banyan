package cn.kennylee.banyan.commons.spring.json;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * 缺少 Jackson 时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingClass("com.fasterxml.jackson.databind.ObjectMapper")
public class JsonMissingClassAutoConfiguration {

    /**
     * 构造实例，并在缺少 Jackson 时输出告警。
     */
    public JsonMissingClassAutoConfiguration() {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Jackson", "banyan.json"));
    }
}
