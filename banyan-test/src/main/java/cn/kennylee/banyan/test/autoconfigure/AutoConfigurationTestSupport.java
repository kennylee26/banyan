package cn.kennylee.banyan.test.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 自动装配测试入口：统一构造 {@link ApplicationContextRunner}。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class AutoConfigurationTestSupport {

    private AutoConfigurationTestSupport() {
    }

    /**
     * 创建自动装配测试上下文。
     *
     * @param autoConfigurations 自动配置类
     * @return 测试上下文
     */
    public static ApplicationContextRunner runner(Class<?>... autoConfigurations) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(autoConfigurations));
    }
}
