package cn.kennylee.banyan.data.mysql.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 缺少 MyBatis-Plus 插件类时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingClass("com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor")
@ConditionalOnProperty(prefix = "banyan.data-mysql", name = "enabled", matchIfMissing = true)
public class MyBatisInterceptorMissingClassAutoConfiguration {

    /**
     * 构造实例。
     */
    public MyBatisInterceptorMissingClassAutoConfiguration() {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "MyBatis-Plus 插件", "banyan-data-mysql"));
    }
}
