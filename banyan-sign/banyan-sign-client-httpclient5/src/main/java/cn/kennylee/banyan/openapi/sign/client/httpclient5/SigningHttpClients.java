package cn.kennylee.banyan.openapi.sign.client.httpclient5;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.util.Objects;

/**
 * HttpClient 5 构建助手：默认禁用重定向与自动重试，并装上签名拦截器。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningHttpClients {

    private SigningHttpClients() {
    }

    /**
     * 创建带签名拦截器的 HttpClient 构建器。
     *
     * @param signer 签名门面
     * @return HttpClient 构建器
     */
    public static HttpClientBuilder builder(OpenApiRequestSigner signer) {
        return HttpClientBuilder.create()
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .addRequestInterceptorLast(new SigningHttpRequestInterceptor(
                        Objects.requireNonNull(signer, "signer")));
    }
}
