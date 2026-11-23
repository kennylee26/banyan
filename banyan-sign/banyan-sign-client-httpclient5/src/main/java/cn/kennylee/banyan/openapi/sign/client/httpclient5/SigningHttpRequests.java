package cn.kennylee.banyan.openapi.sign.client.httpclient5;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.SignMessages;
import cn.kennylee.banyan.openapi.sign.client.QueryParams;
import cn.kennylee.banyan.openapi.sign.client.SignedRequest;
import cn.kennylee.banyan.openapi.sign.client.UnsignedRequest;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 原地签名：把已构造好的 HttpClient5 请求加密、签名并写回 header。
 *
 * <p>调用后可直接用任意 HttpClient 发送，不需要构造签名专用 client：</p>
 * <pre>{@code
 * HttpPost post = new HttpPost(url);
 * post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
 * SigningHttpRequests.sign(signer, post);
 * httpClient.execute(post);
 * }</pre>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningHttpRequests {

    private SigningHttpRequests() {
    }

    /**
     * 原地签名请求。
     *
     * @param signer  签名门面
     * @param request 已构造好的经典请求
     * @throws IOException             读取或替换 entity 失败
     * @throws IllegalArgumentException URI 非法或 entity 类型不受支持
     */
    public static void sign(OpenApiRequestSigner signer, ClassicHttpRequest request)
            throws IOException {
        Objects.requireNonNull(signer, "signer");
        Objects.requireNonNull(request, "request");
        URI uri = uriOf(request);
        HttpEntity current = request.getEntity();
        SignedRequest signed = signer.sign(new UnsignedRequest(request.getMethod(),
                uri.getRawPath(), QueryParams.parse(uri.getRawQuery()), readBody(current)));
        if (signed.body() != null) {
            byte[] signedBody = signed.body().getBytes(StandardCharsets.UTF_8);
            request.setEntity(new ByteArrayEntity(signedBody, contentTypeOf(current)));
            // 拦截器场景下 Content-Length 已按原文计算过，需要同步更新；原地签名时
            // 交给 HttpClient 在发送前按新 entity 计算，显式设置会触发重复 header 错误
            if (request.containsHeader(HttpHeaders.CONTENT_LENGTH)) {
                request.setHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(signedBody.length));
            }
        }
        signed.headers().forEach(request::setHeader);
    }

    /** 解析请求 URI；缺失或非法时明确失败。 */
    private static URI uriOf(ClassicHttpRequest request) {
        try {
            URI uri = request.getUri();
            if (uri == null) {
                throw new IllegalArgumentException(SignMessages.URI_MISSING);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(SignMessages.URI_INVALID, e);
        }
    }

    /** 读取可重复 entity 的 UTF-8 文本内容；类型不受支持时明确失败。 */
    private static String readBody(HttpEntity entity) throws IOException {
        if (entity == null) {
            return null;
        }
        if (!(entity instanceof StringEntity) && !(entity instanceof ByteArrayEntity)) {
            throw new IllegalArgumentException(SignMessages.HTTPCLIENT_ENTITY_UNSUPPORTED);
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
