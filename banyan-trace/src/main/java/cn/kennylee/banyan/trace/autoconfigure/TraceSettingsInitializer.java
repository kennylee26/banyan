package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.trace.support.TraceSettings;
import org.springframework.beans.factory.InitializingBean;

/**
 * 启动期把配置写入 SPI 与静态工具可读取的设置持有者。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class TraceSettingsInitializer implements InitializingBean {

    /** 配置。 */
    private final TraceProperties properties;

    /**
     * 构造实例。
     *
     * @param properties 配置
     */
    public TraceSettingsInitializer(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        TraceSettings.init(properties);
    }
}
