package cn.kennylee.banyan.openapi.sign.autoconfigure;

import cn.kennylee.banyan.commons.crypto.AesGcmPayloadCipher;
import cn.kennylee.banyan.commons.crypto.HmacSha256SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.InMemoryNonceStore;
import cn.kennylee.banyan.commons.crypto.NonceStore;
import cn.kennylee.banyan.commons.crypto.PayloadCipher;
import cn.kennylee.banyan.commons.crypto.ReplayGuard;
import cn.kennylee.banyan.commons.crypto.SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.openapi.sign.support.ConfiguredSignKeyResolver;
import cn.kennylee.banyan.openapi.sign.support.SignKeyResolver;
import cn.kennylee.banyan.openapi.sign.web.OpenApiSignInterceptor;
import cn.kennylee.banyan.openapi.sign.web.RepeatableBodyFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/**
 * OpenAPI 验签解密自动配置：默认开启，使用方自定义 bean 优先。
 *
 * <p>缺少 Servlet 或 Spring MVC 时不注册任何 bean，由
 * {@link OpenApiSignMissingClassAutoConfiguration} 输出告警。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {
        "jakarta.servlet.Filter",
        "org.springframework.web.servlet.HandlerInterceptor"})
@ConditionalOnProperty(prefix = "banyan.openapi-sign", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenApiSignProperties.class)
public class OpenApiSignAutoConfiguration {

    /** 参数名：签名算法。 */
    private static final String FIELD_ALGORITHM = "algorithm";

    /** 参数名：加密算法。 */
    private static final String FIELD_CIPHER = "cipher";

    /**
     * 签名 header 名配置。
     *
     * @param properties 配置
     * @return header 名配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SignHeaders openApiSignHeaders(OpenApiSignProperties properties) {
        return properties.toSignHeaders();
    }

    /**
     * 签名算法，当前只支持 HMAC-SHA256。
     *
     * @param properties 配置
     * @return 签名算法
     * @throws IllegalArgumentException 配置了未支持的算法
     */
    @Bean
    @ConditionalOnMissingBean
    public SignAlgorithm openApiSignAlgorithm(OpenApiSignProperties properties) {
        requireSupported(FIELD_ALGORITHM, HmacSha256SignAlgorithm.NAME,
                properties.getSignAlgorithm());
        return new HmacSha256SignAlgorithm();
    }

    /**
     * nonce 存储，默认内存实现；多实例部署请自行提供 bean。
     *
     * @return nonce 存储
     */
    @Bean
    @ConditionalOnMissingBean
    public NonceStore openApiSignNonceStore() {
        return new InMemoryNonceStore();
    }

    /**
     * 请求体加密实现，默认 AES-256-GCM。
     *
     * @param properties 配置
     * @return 加密实现
     * @throws IllegalArgumentException 配置了未支持的算法
     */
    @Bean
    @ConditionalOnMissingBean
    public PayloadCipher openApiSignCipher(OpenApiSignProperties properties) {
        requireSupported(FIELD_CIPHER, AesGcmPayloadCipher.NAME, properties.getCipher());
        return new AesGcmPayloadCipher();
    }

    /**
     * 密钥解析，默认读取配置。
     *
     * @param properties 配置
     * @return 密钥解析
     */
    @Bean
    @ConditionalOnMissingBean
    public SignKeyResolver openApiSignKeyResolver(OpenApiSignProperties properties) {
        return new ConfiguredSignKeyResolver(properties.getKeys());
    }

    /**
     * 验签守卫。
     *
     * @param algorithm   签名算法
     * @param nonceStore  nonce 存储
     * @param headers     header 名配置
     * @param properties  配置
     * @return 验签守卫
     */
    @Bean
    @ConditionalOnMissingBean
    public ReplayGuard openApiSignReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore,
                                              SignHeaders headers,
                                              OpenApiSignProperties properties) {
        return new ReplayGuard(algorithm, nonceStore, Clock.systemUTC(),
                properties.getWindow().toMillis(), headers);
    }

    /**
     * 可重复读 body 过滤器。
     *
     * @return 过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public RepeatableBodyFilter openApiSignRepeatableBodyFilter() {
        return new RepeatableBodyFilter();
    }

    /**
     * 验签解密拦截器。
     *
     * @param properties   配置
     * @param guard        验签守卫
     * @param keyResolver  密钥解析
     * @param headers      header 名配置
     * @param cipher       加密实现
     * @return 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenApiSignInterceptor openApiSignInterceptor(OpenApiSignProperties properties,
                                                         ReplayGuard guard,
                                                         SignKeyResolver keyResolver,
                                                         SignHeaders headers,
                                                         PayloadCipher cipher) {
        return new OpenApiSignInterceptor(properties, guard, keyResolver, headers, cipher);
    }

    /**
     * 把拦截器注册到 Spring MVC。
     *
     * @param interceptor 验签解密拦截器
     * @return MVC 配置
     */
    @Bean
    public WebMvcConfigurer openApiSignWebMvcConfigurer(OpenApiSignInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    /** 校验算法标识受支持，否则抛带字段名的参数异常。 */
    private static void requireSupported(String field, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, field));
        }
    }
}
