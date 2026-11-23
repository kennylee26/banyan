package cn.kennylee.banyan.openapi.sign.client.resttemplate;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.QueryParams;
import cn.kennylee.banyan.openapi.sign.client.SignedRequest;
import cn.kennylee.banyan.openapi.sign.client.UnsignedRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * RestTemplate 请求拦截器：读取实际请求，加密 body 并附加五个签名 header。
 *
 * <p>Spring 已把请求体转为 {@code byte[]} 传入，因此无需额外配置缓冲工厂；
 * multipart 请求不受支持，会直接抛 {@link IllegalArgumentException}。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    /** 签名门面。 */
    private final OpenApiRequestSigner signer;

    /**
     * 构造拦截器。
     *
     * @param signer 签名门面
     */
    public SigningClientHttpRequestInterceptor(OpenApiRequestSigner signer) {
        this.signer = Objects.requireNonNull(signer, "signer");
    }

    /**
     * 在请求发出前加密、签名并写回。
     *
     * @param request   请求
     * @param body      请求体字节
     * @param execution 执行链
     * @return 响应
     * @throws IOException              IO 异常
     * @throws IllegalArgumentException multipart 请求不受支持
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        MediaType contentType = request.getHeaders().getContentType();
        if (contentType != null && MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
            throw new IllegalArgumentException("RestTemplate 适配器不支持 multipart 请求");
        }
        URI uri = request.getURI();
        String text = body == null ? null : new String(body, StandardCharsets.UTF_8);
        SignedRequest signed = signer.sign(new UnsignedRequest(request.getMethod().name(),
                uri.getRawPath(), QueryParams.parse(uri.getRawQuery()), text));
        signed.headers().forEach((name, value) -> request.getHeaders().set(name, value));
        byte[] signedBody = signed.body() == null
                ? null : signed.body().getBytes(StandardCharsets.UTF_8);
        return execution.execute(request, signedBody == null ? new byte[0] : signedBody);
    }
}
