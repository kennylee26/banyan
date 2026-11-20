package cn.kennylee.banyan.commons.crypto;

import java.time.Clock;
import java.util.Objects;

/**
 * 调用方签名器：把「补充协议参数、规范化、计算签名、组装待发送数据」收拢为一步。
 *
 * <p>它只准备数据，不发送 HTTP 请求，也不绑定任何 HTTP 客户端。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class RequestSigner {

    /** 签名算法。 */
    private final SignAlgorithm algorithm;

    /** 时钟。 */
    private final Clock clock;

    /** HTTP header 名配置。 */
    private final SignHeaders headersConfig;

    /**
     * 使用默认 HMAC-SHA256 算法、系统时钟与默认 header 名。
     */
    public RequestSigner() {
        this(new HmacSha256SignAlgorithm(), Clock.systemUTC(), SignHeaders.DEFAULT);
    }

    /**
     * 使用指定算法、系统时钟与默认 header 名。
     *
     * @param algorithm 签名算法
     */
    public RequestSigner(SignAlgorithm algorithm) {
        this(algorithm, Clock.systemUTC(), SignHeaders.DEFAULT);
    }

    /**
     * 使用默认算法、系统时钟与自定义 header 名。
     *
     * @param headersConfig header 名配置
     */
    public RequestSigner(SignHeaders headersConfig) {
        this(new HmacSha256SignAlgorithm(), Clock.systemUTC(), headersConfig);
    }

    /**
     * 使用指定算法与系统时钟、默认 header 名。
     *
     * @param algorithm 签名算法
     * @param clock     时钟
     */
    public RequestSigner(SignAlgorithm algorithm, Clock clock) {
        this(algorithm, clock, SignHeaders.DEFAULT);
    }

    /**
     * 构造签名器。
     *
     * @param algorithm     签名算法
     * @param clock         时钟
     * @param headersConfig header 名配置
     */
    public RequestSigner(SignAlgorithm algorithm, Clock clock, SignHeaders headersConfig) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.headersConfig = Objects.requireNonNull(headersConfig, "headersConfig");
    }

    /**
     * 为调用第三方的请求生成完整签名数据。
     *
     * @param request 待签名请求
     * @param key     密钥
     * @return 原始请求与附加签名参数
     */
    public SignedRequest sign(UnsignedRequest request, SignKey key) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(key, "key");

        long timestamp = clock.millis();
        String nonce = Signs.newNonce();
        SignRequest prepared = new SignRequest(request.method(), request.path(),
                request.queryParams(), request.body(), timestamp, nonce, key.keyId(),
                algorithm.name(), null);
        String signature = algorithm.sign(Signs.canonicalize(prepared), key.secret());
        return new SignedRequest(request,
                Signs.authParams(algorithm.name(), key.keyId(), timestamp, nonce, signature),
                headersConfig);
    }
}
