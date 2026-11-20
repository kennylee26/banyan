package cn.kennylee.banyan.data.mysql.autoconfigure;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自定义 InnerInterceptor 追加规则测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MyBatisInterceptorExtensionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MyBatisDasAutoConfiguration.class,
                    MyBatisDasMissingClassAutoConfiguration.class,
                    MyBatisInterceptorAutoConfiguration.class,
                    MyBatisInterceptorMissingClassAutoConfiguration.class));

    /** 默认插件与自定义插件共存，自定义排在最后。 */
    @Test
    void appendsCustomInterceptorAfterBuiltIn() {
        // when
        runner.withBean("custom", InnerInterceptor.class, CustomInterceptor::new)
                .run(context -> {
                    List<InnerInterceptor> interceptors =
                            context.getBean(MybatisPlusInterceptor.class).getInterceptors();
                    assertThat(interceptors).hasSize(2);
                    assertThat(interceptors.get(0)).isInstanceOf(PaginationInnerInterceptor.class);
                    assertThat(interceptors.get(1)).isInstanceOf(CustomInterceptor.class);
                });
    }

    /** 关闭全部自带开关后，容器只含自定义插件。 */
    @Test
    void keepsOnlyCustomInterceptorWhenBuiltInsDisabled() {
        // when
        runner.withPropertyValues(
                        "banyan.data-mysql.pagination.enabled=false",
                        "banyan.data-mysql.like-escape.inner-interceptor.enabled=false",
                        "banyan.data-mysql.block-attack.enabled=false")
                .withBean("custom", InnerInterceptor.class, CustomInterceptor::new)
                .run(context -> {
                    List<InnerInterceptor> interceptors =
                            context.getBean(MybatisPlusInterceptor.class).getInterceptors();
                    assertThat(interceptors).hasSize(1);
                    assertThat(interceptors.get(0)).isInstanceOf(CustomInterceptor.class);
                });
    }

    /** 使用者已有容器时让位，零散插件不被追加。 */
    @Test
    void doesNotAppendWhenUserContainerExists() {
        // when
        runner.withBean(MybatisPlusInterceptor.class, MybatisPlusInterceptor::new)
                .withBean("custom", InnerInterceptor.class, CustomInterceptor::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).isEmpty();
                });
    }

    /** 多个自定义插件按 Ordered 升序排列。 */
    @Test
    void ordersCustomInterceptorsByOrder() {
        // when
        runner.withBean("low", InnerInterceptor.class, LowPriorityInterceptor::new)
                .withBean("high", InnerInterceptor.class, HighPriorityInterceptor::new)
                .run(context -> {
                    List<InnerInterceptor> interceptors =
                            context.getBean(MybatisPlusInterceptor.class).getInterceptors();
                    assertThat(interceptors).hasSize(3);
                    assertThat(interceptors.get(0)).isInstanceOf(PaginationInnerInterceptor.class);
                    assertThat(interceptors.get(1)).isInstanceOf(HighPriorityInterceptor.class);
                    assertThat(interceptors.get(2)).isInstanceOf(LowPriorityInterceptor.class);
                });
    }

    /** 自定义插件。 */
    static class CustomInterceptor implements InnerInterceptor {
    }

    /** 高优先级自定义插件。 */
    static class HighPriorityInterceptor implements InnerInterceptor, Ordered {

        @Override
        public int getOrder() {
            return 10;
        }
    }

    /** 低优先级自定义插件。 */
    static class LowPriorityInterceptor implements InnerInterceptor, Ordered {

        @Override
        public int getOrder() {
            return 20;
        }
    }
}
