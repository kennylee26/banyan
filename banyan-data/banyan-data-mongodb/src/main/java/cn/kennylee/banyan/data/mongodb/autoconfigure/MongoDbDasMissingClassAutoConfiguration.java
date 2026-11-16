package cn.kennylee.banyan.data.mongodb.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * 缺少 Spring Data MongoDB 时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingClass("org.springframework.data.mongodb.core.MongoTemplate")
public class MongoDbDasMissingClassAutoConfiguration {

    /**
     * 构造实例。
     */
    public MongoDbDasMissingClassAutoConfiguration() {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Spring Data MongoDB", "banyan-data-mongodb"));
    }
}
