package cn.kennylee.banyan.mq.producer.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少事务类时只打 WARN，不注册 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnExpression("${banyan.mq.enabled:true} and ${banyan.mq.producer.enabled:true}")
public class MqProducerMissingClassAutoConfiguration {

    /**
     * 缺少 Spring Transaction。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.transaction.support.TransactionSynchronization")
    static class TransactionMissing {

        /**
         * 构造并告警。
         */
        TransactionMissing() {
            log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Spring Transaction",
                    "banyan-mq-producer"));
        }
    }
}
