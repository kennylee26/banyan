package cn.kennylee.banyan.openapi.sign.client.jdkhttp;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.QueryParams;
import cn.kennylee.banyan.openapi.sign.client.SignedRequest;
import cn.kennylee.banyan.openapi.sign.client.UnsignedRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
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
     */
    public static HttpRequest sign(OpenApiRequestSigner signer, HttpRequest request, String body) {
        Objects.requireNonNull(signer, "signer");
        Objects.requireNonNull(request, "request");
        URI uri = request.uri();
        SignedRequest signed = signer.sign(new UnsignedRequest(request.method(),
                uri.getRawPath(), QueryParams.parse(uri.getRawQuery()), body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(request, (name, value) -> true)
                .method(request.method(), publisherOf(signed.body()));
        signed.headers().forEach(builder::setHeader);
        return builder.build();
    }

    /** 空 body 用 noBody，其余按 UTF-8 编码。 */
    private static HttpRequest.BodyPublisher publisherOf(String body) {
        return body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }
}
