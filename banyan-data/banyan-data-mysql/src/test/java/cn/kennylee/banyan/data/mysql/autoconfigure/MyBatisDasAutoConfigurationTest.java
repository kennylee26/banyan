package cn.kennylee.banyan.data.mysql.autoconfigure;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL 自动配置测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class MyBatisDasAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MyBatisDasAutoConfiguration.class,
                    MyBatisDasMissingClassAutoConfiguration.class));

    /** 默认加载。 */
    @Test
    void loadsByDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(DasQueryStrategyRegistry.class));
    }

    /** 关闭开关后不加载。 */
    @Test
    void honorsEnabledFalse() {
        runner.withPropertyValues("banyan.data-mysql.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DasQueryStrategyRegistry.class));
    }

    /** 缺类时跳过并告警。 */
    @Test
    void skipsAndWarnsWhenRequiredClassMissing(CapturedOutput output) {
        runner.withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus.core.mapper.BaseMapper"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DasQueryStrategyRegistry.class);
                });
        assertThat(output).contains("MyBatis-Plus");
    }

    /** 使用者已有 bean 时让位。 */
    @Test
    void backsOffWhenUserBeanExists() {
        runner.withBean(DasQueryStrategyRegistry.class,
                        () -> new DefaultDasQueryStrategyRegistry(Map.of()))
                .run(context -> assertThat(context.getBeansOfType(DasQueryStrategyRegistry.class)).hasSize(1));
    }

    /** 运行时初始化失败即失败。 */
    @Test
    void failsFastWhenRuntimeInitializationFails() {
        runner.withBean(DasQueryStrategyRegistry.class, () -> {
                    throw new IllegalStateException("boom");
                })
                .run(context -> assertThat(context).hasFailed());
    }
}
