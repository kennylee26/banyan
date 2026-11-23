package cn.kennylee.banyan.openapi.sign.client.httpclient5;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.SignMessages;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Objects;

/**
 * HttpClient 5 请求拦截器：把请求交给 {@link SigningHttpRequests} 完成加密与签名。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningHttpRequestInterceptor implements HttpRequestInterceptor {

    /** 签名门面。 */
    private final OpenApiRequestSigner signer;

    /**
     * 构造拦截器。
     *
     * @param signer 签名门面
     */
    public SigningHttpRequestInterceptor(OpenApiRequestSigner signer) {
        this.signer = Objects.requireNonNull(signer, "signer");
    }

    /**
     * 在请求发出前加密、签名并写回。
     *
     * @param request 请求
     * @param entity  实体信息
     * @param context 上下文
     * @throws HttpException            HTTP 异常
     * @throws IOException              IO 异常
     * @throws IllegalArgumentException 不是经典请求，或 entity 类型不受支持
     */
    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        if (!(request instanceof ClassicHttpRequest classic)) {
            throw new IllegalArgumentException(SignMessages.HTTPCLIENT_NOT_CLASSIC);
        }
        SigningHttpRequests.sign(signer, classic);
    }
}
