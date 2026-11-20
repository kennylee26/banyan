package cn.kennylee.banyan.openapi.sign.autoconfigure;

import cn.kennylee.banyan.commons.crypto.NonceStore;
import cn.kennylee.banyan.commons.crypto.PayloadCipher;
import cn.kennylee.banyan.commons.crypto.ReplayGuard;
import cn.kennylee.banyan.commons.crypto.SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.SignKey;
import cn.kennylee.banyan.openapi.sign.support.SignKeyResolver;
import cn.kennylee.banyan.openapi.sign.web.OpenApiSignInterceptor;
import cn.kennylee.banyan.openapi.sign.web.RepeatableBodyFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 签名自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenApiSignAutoConfigurationTest {

    /** Servlet 环境下的运行器。 */
    private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenApiSignAutoConfiguration.class,
                    OpenApiSignMissingClassAutoConfiguration.class));

    /** 默认装配全部 bean。 */
    @Test
    void loadsByDefault() {
        // when
        servletRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenApiSignProperties.class);
            assertThat(context).hasSingleBean(SignHeaders.class);
            assertThat(context).hasSingleBean(SignAlgorithm.class);
            assertThat(context).hasSingleBean(NonceStore.class);
            assertThat(context).hasSingleBean(PayloadCipher.class);
            assertThat(context).hasSingleBean(SignKeyResolver.class);
            assertThat(context).hasSingleBean(ReplayGuard.class);
            assertThat(context).hasSingleBean(RepeatableBodyFilter.class);
            assertThat(context).hasSingleBean(OpenApiSignInterceptor.class);
            assertThat(context.getBeansOfType(WebMvcConfigurer.class)).hasSize(1);
        });
    }

    /** 总开关关闭时不注册任何 bean。 */
    @Test
    void skipsEverythingWhenDisabled() {
        // when
        servletRunner.withPropertyValues("banyan.openapi-sign.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenApiSignProperties.class);
                    assertThat(context).doesNotHaveBean(RepeatableBodyFilter.class);
                    assertThat(context).doesNotHaveBean(OpenApiSignInterceptor.class);
                    assertThat(context).doesNotHaveBean(ReplayGuard.class);
                });
    }

    /** 缺少 Spring MVC 时跳过装配并告警。 */
    @Test
    void skipsWhenSpringMvcMissing(CapturedOutput output) {
        // when
        servletRunner.withClassLoader(
                        new FilteredClassLoader("org.springframework.web.servlet.HandlerInterceptor"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(OpenApiSignInterceptor.class);
                    assertThat(context).doesNotHaveBean(RepeatableBodyFilter.class);
                });

        // then
        assertThat(output).contains("Spring MVC");
        assertThat(output).contains("banyan-openapi-sign");
    }

    /** 缺少 Servlet 时只告警。 */
    @Test
    void warnsWhenServletMissing(CapturedOutput output) {
        // when
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("jakarta.servlet.Filter"))
                .withConfiguration(AutoConfigurations.of(
                        OpenApiSignMissingClassAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());

        // then
        assertThat(output).contains("Servlet");
        assertThat(output).contains("banyan-openapi-sign");
    }

    /** 使用方自定义密钥解析、nonce 存储与加密实现时优先。 */
    @Test
    void backsOffForCustomBeans() {
        // given
        SignKeyResolver customResolver = keyId -> new SignKey(keyId, "custom-secret");
        NonceStore customStore = (nonceKey, ttlMillis) -> true;
        PayloadCipher customCipher = new PayloadCipher() {

            @Override
            public String name() {
                return "custom";
            }

            @Override
            public String encrypt(String plaintext, String secret) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String secret) {
                return ciphertext;
            }
        };

        // when
        servletRunner
                .withBean(SignKeyResolver.class, () -> customResolver)
                .withBean(NonceStore.class, () -> customStore)
                .withBean(PayloadCipher.class, () -> customCipher)
                .run(context -> {
                    assertThat(context.getBean(SignKeyResolver.class)).isSameAs(customResolver);
                    assertThat(context.getBean(NonceStore.class)).isSameAs(customStore);
                    assertThat(context.getBean(PayloadCipher.class)).isSameAs(customCipher);
                });
    }

    /** 未支持的签名算法配置在启动期失败。 */
    @Test
    void failsFastForUnknownSignAlgorithm() {
        // when
        servletRunner.withPropertyValues("banyan.openapi-sign.sign-algorithm=MD5")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("algorithm");
                });
    }

    /** 未支持的加密算法配置在启动期失败。 */
    @Test
    void failsFastForUnknownCipher() {
        // when
        servletRunner.withPropertyValues("banyan.openapi-sign.cipher=sm4")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("cipher");
                });
    }
}
