package cn.kennylee.banyan.openapi.sign.client.resttemplate;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.util.Objects;

/**
 * RestTemplate 构建助手：使用 JDK HttpClient 且禁止跟随重定向，并装上签名拦截器。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningRestTemplates {

    private SigningRestTemplates() {
    }

    /**
     * 创建带签名拦截器的 RestTemplate。
     *
     * @param signer 签名门面
     * @return RestTemplate
     */
    public static RestTemplate create(OpenApiRequestSigner signer) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory(httpClient));
        restTemplate.getInterceptors().add(new SigningClientHttpRequestInterceptor(
                Objects.requireNonNull(signer, "signer")));
        return restTemplate;
    }
}
