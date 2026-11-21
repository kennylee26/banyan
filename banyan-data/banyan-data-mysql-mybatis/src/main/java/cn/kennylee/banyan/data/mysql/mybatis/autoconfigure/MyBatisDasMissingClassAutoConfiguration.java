package cn.kennylee.banyan.data.mysql.mybatis.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * 缺少 MyBatis-Plus 时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingClass("com.baomidou.mybatisplus.core.mapper.BaseMapper")
public class MyBatisDasMissingClassAutoConfiguration {

    /**
     * 构造实例。
     */
    public MyBatisDasMissingClassAutoConfiguration() {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "MyBatis-Plus", "banyan-data-mysql-mybatis"));
    }
}
