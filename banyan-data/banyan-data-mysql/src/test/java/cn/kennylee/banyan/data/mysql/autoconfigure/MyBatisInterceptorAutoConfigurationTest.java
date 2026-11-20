package cn.kennylee.banyan.data.mysql.autoconfigure;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件容器自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class MyBatisInterceptorAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MyBatisDasAutoConfiguration.class,
                    MyBatisDasMissingClassAutoConfiguration.class,
                    MyBatisInterceptorAutoConfiguration.class,
                    MyBatisInterceptorMissingClassAutoConfiguration.class));

    /** 默认装配：容器含分页，不含 LIKE 转义与阻断。 */
    @Test
    void createsContainerWithPaginationByDefault() {
        // when
        runner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            List<InnerInterceptor> interceptors = context.getBean(MybatisPlusInterceptor.class).getInterceptors();
            assertThat(interceptors).hasSize(1);
            assertThat(interceptors.get(0)).isInstanceOf(PaginationInnerInterceptor.class);
        });
    }

    /** 关闭分页：容器仍可存在，但不含分页插件。 */
    @Test
    void skipsPaginationWhenDisabled() {
        // when
        runner.withPropertyValues("banyan.data-mysql.pagination.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                            .noneMatch(interceptor -> interceptor instanceof PaginationInnerInterceptor);
                });
    }

    /** 关闭模块总开关：不创建插件容器，也不创建查询策略 bean。 */
    @Test
    void skipsEverythingWhenModuleDisabled() {
        // when
        runner.withPropertyValues("banyan.data-mysql.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MybatisPlusInterceptor.class);
                    assertThat(context).doesNotHaveBean(DasQueryStrategyRegistry.class);
                });
    }

    /** 使用者已有容器：整库让位，不追加自己的插件。 */
    @Test
    void backsOffWhenUserContainerExists() {
        // when
        runner.withBean(MybatisPlusInterceptor.class, MybatisPlusInterceptor::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).isEmpty();
                });
    }

    /** 排除插件类：启动不失败，并输出 WARN。 */
    @Test
    void skipsAndWarnsWhenPluginClassMissing(CapturedOutput output) {
        // when
        runner.withClassLoader(new FilteredClassLoader(
                        "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(MybatisPlusInterceptor.class);
                });

        // then
        assertThat(output).contains("MyBatis-Plus 插件");
    }

    /** 显式开启阻断：容器内出现阻断插件。 */
    @Test
    void registersBlockAttackWhenExplicitlyEnabled() {
        // when
        runner.withPropertyValues("banyan.data-mysql.block-attack.enabled=true")
                .run(context -> assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                        .anyMatch(interceptor -> interceptor instanceof BlockAttackInnerInterceptor));
    }
}
