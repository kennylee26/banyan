package cn.kennylee.banyan.test.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配测试支持测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class AutoConfigurationTestSupportTest {

    /** 默认加载。 */
    @Test
    void loadsByDefault() {
        // when
        AutoConfigurationTestSupport.runner(DemoAutoConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(DemoService.class));
    }

    /** 关闭开关后不加载。 */
    @Test
    void honorsEnabledFalse() {
        // when
        AutoConfigurationTestSupport.runner(DemoAutoConfiguration.class)
                .withPropertyValues("banyan.demo.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DemoService.class));
    }

    /** 缺类时跳过且正常启动。 */
    @Test
    void skipsWhenRequiredClassMissingAndStartsNormally() {
        // when
        AutoConfigurationTestSupport.runner(DemoAutoConfiguration.class)
                .withClassLoader(new FilteredClassLoader(DemoRequiredClass.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DemoService.class);
                });
    }

    /** 使用者自定义 bean 时让位。 */
    @Test
    void backsOffWhenUserDefinesBean() {
        // when
        AutoConfigurationTestSupport.runner(DemoAutoConfiguration.class)
                .withUserConfiguration(UserConfig.class)
                .run(context -> assertThat(context.getBean(DemoService.class).name()).isEqualTo("user"));
    }

    @AutoConfiguration
    @ConditionalOnClass(name = "cn.kennylee.banyan.test.autoconfigure.AutoConfigurationTestSupportTest$DemoRequiredClass")
    @ConditionalOnProperty(prefix = "banyan.demo", name = "enabled", matchIfMissing = true)
    static class DemoAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean
        DemoService demoService() {
            return new DemoService("auto");
        }
    }

    @TestConfiguration
    static class UserConfig {
        @Bean
        DemoService demoService() {
            return new DemoService("user");
        }
    }

    record DemoService(String name) {
    }

    static class DemoRequiredClass {
    }
}
