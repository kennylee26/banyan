package cn.kennylee.banyan.openapi.sign.client.jdkhttp;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.QueryParams;
import cn.kennylee.banyan.openapi.sign.client.SignedRequest;
import cn.kennylee.banyan.openapi.sign.client.UnsignedRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * JDK HttpClient 签名助手：返回一个新的已签名请求，HttpClient 由调用方自行构造。
 *
 * <p>JDK 的 {@link HttpRequest.BodyPublisher} 不暴露原始内容，因此 body 必须由调用方
 * 作为字符串单独传入；空 body 传 null。</p>
 *
 * <pre>{@code
 * HttpRequest unsigned = HttpRequest.newBuilder(uri).build();
 * HttpRequest signed = SigningJdkHttpRequests.sign(signer, unsigned, json);
 * client.send(signed, BodyHandlers.ofString());
 * }</pre>
 *
 * <p>本类不包装 HttpClient：重定向与重试行为由调用方的 client 决定。JDK HttpClient
 * 默认不跟随重定向，保持默认即可；签名请求不要额外配置自动重试。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SigningJdkHttpRequests {

    private SigningJdkHttpRequests() {
    }

    /**
     * 生成已签名请求。
     *
     * @param signer  签名门面
     * @param request 已构造的请求；其 method、URI、version、timeout 与 header 会被保留
     * @param body    请求体原文，可为 null
     * @return 新的已签名请求
     * @throws IllegalArgumentException multipart 请求不受支持
     */
    public static HttpRequest sign(OpenApiRequestSigner signer, HttpRequest request, String body) {
        Objects.requireNonNull(signer, "signer");
        Objects.requireNonNull(request, "request");
        if (isMultipart(request)) {
            throw new IllegalArgumentException("JDK HttpClient 适配器不支持 multipart 请求");
        }
        URI uri = request.uri();
        SignedRequest signed = signer.sign(new UnsignedRequest(request.method(),
                uri.getRawPath(), QueryParams.parse(uri.getRawQuery()), body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(request, (name, value) -> true)
                .method(request.method(), publisherOf(signed.body()));
        signed.headers().forEach(builder::setHeader);
        return builder.build();
    }

    /** Content-Type 为 multipart 时不支持签名。 */
    private static boolean isMultipart(HttpRequest request) {
        return request.headers().firstValue("Content-Type")
                .map(value -> value.toLowerCase(Locale.ROOT).startsWith("multipart/"))
                .orElse(false);
    }

    /** 空 body 用 noBody，其余按 UTF-8 编码。 */
    private static HttpRequest.BodyPublisher publisherOf(String body) {
        return body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }
}
