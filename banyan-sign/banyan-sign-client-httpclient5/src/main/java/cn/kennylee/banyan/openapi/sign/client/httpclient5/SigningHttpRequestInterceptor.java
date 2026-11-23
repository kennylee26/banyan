package cn.kennylee.banyan.openapi.sign.client.httpclient5;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.QueryParams;
import cn.kennylee.banyan.openapi.sign.client.SignedRequest;
import cn.kennylee.banyan.openapi.sign.client.UnsignedRequest;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * HttpClient 5 请求拦截器：读取实际请求，加密 body 并附加五个签名 header。
 *
 * <p>只支持可重复读取的 entity（{@code StringEntity}、{@code ByteArrayEntity} 等）；
 * 流式或 multipart entity 会直接抛 {@link IllegalArgumentException}。</p>
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
     * @throws HttpException HTTP 异常
     * @throws IOException   IO 异常
     */
    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        if (!(request instanceof ClassicHttpRequest classic)) {
            throw new IllegalArgumentException("仅支持经典 HttpClient5 请求");
        }
        URI uri = uriOf(request);
        HttpEntity current = classic.getEntity();
        SignedRequest signed = signer.sign(new UnsignedRequest(request.getMethod(),
                uri.getRawPath(), QueryParams.parse(uri.getRawQuery()), readBody(current)));
        if (signed.body() != null) {
            byte[] signedBody = signed.body().getBytes(StandardCharsets.UTF_8);
            classic.setEntity(new ByteArrayEntity(signedBody, contentTypeOf(current)));
            request.setHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(signedBody.length));
        }
        signed.headers().forEach(request::setHeader);
    }

    /** 解析请求 URI；缺失或非法时明确失败。 */
    private static URI uriOf(HttpRequest request) {
        try {
            URI uri = request.getUri();
            if (uri == null) {
                throw new IllegalArgumentException("请求缺少 URI");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("请求 URI 非法", e);
        }
    }

    /** 读取可重复 entity 的 UTF-8 文本内容；类型不受支持时明确失败。 */
    private static String readBody(HttpEntity entity) throws IOException {
        if (entity == null) {
            return null;
        }
        if (!(entity instanceof StringEntity) && !(entity instanceof ByteArrayEntity)) {
            throw new IllegalArgumentException(
                    "HttpClient5 只支持 StringEntity 或 ByteArrayEntity，流式、文件与 multipart entity 不受支持");
        }
        return new String(EntityUtils.toByteArray(entity), StandardCharsets.UTF_8);
    }

    /** 解析 entity 的 Content-Type；缺失返回 null。 */
    private static ContentType contentTypeOf(HttpEntity entity) {
        if (entity == null || entity.getContentType() == null) {
            return null;
        }
        return ContentType.parseLenient(entity.getContentType());
    }
}
