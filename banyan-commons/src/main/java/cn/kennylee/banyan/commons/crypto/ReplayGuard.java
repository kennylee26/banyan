package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

/**
 * 入站验签与防重放：时间窗口 → 签名校验 → nonce 去重，任一失败统一返回
 * {@link VerifyResult#INVALID}。
 *
 * <p>失败顺序有意如此：先判时间窗口，避免过期请求消耗签名计算与 nonce 存储；
 * 签名通过后才登记 nonce，避免攻击者用坏签名耗尽存储。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class ReplayGuard {

    /** 默认时间窗口：5 分钟。 */
    public static final long DEFAULT_WINDOW_MILLIS = 5L * 60L * 1000L;

    /** 参数名：时间窗口。 */
    private static final String FIELD_WINDOW = "window";

    /** 签名算法。 */
    private final SignAlgorithm algorithm;

    /** nonce 存储。 */
    private final NonceStore nonceStore;

    /** 时钟。 */
    private final Clock clock;

    /** 时间窗口（毫秒）。 */
    private final long windowMillis;

    /** HTTP header 名配置。 */
    private final SignHeaders headersConfig;

    /**
     * 使用默认窗口、系统时钟与默认 header 名构造实例。
     *
     * @param algorithm  签名算法
     * @param nonceStore nonce 存储
     */
    public ReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore) {
        this(algorithm, nonceStore, Clock.systemUTC(), DEFAULT_WINDOW_MILLIS,
                SignHeaders.DEFAULT);
    }

    /**
     * 使用指定窗口与系统时钟、默认 header 名构造实例。
     *
     * @param algorithm    签名算法
     * @param nonceStore   nonce 存储
     * @param clock        时钟
     * @param windowMillis 时间窗口毫秒数，必须为正
     * @throws IllegalArgumentException 窗口非正
     */
    public ReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore, Clock clock,
                       long windowMillis) {
        this(algorithm, nonceStore, clock, windowMillis, SignHeaders.DEFAULT);
    }

    /**
     * 构造实例。
     *
     * @param algorithm     签名算法
     * @param nonceStore    nonce 存储
     * @param clock         时钟
     * @param windowMillis  时间窗口毫秒数，必须为正
     * @param headersConfig header 名配置，须与调用方共用
     * @throws IllegalArgumentException 窗口非正
     */
    public ReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore, Clock clock,
                       long windowMillis, SignHeaders headersConfig) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, FIELD_WINDOW));
        }
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.nonceStore = Objects.requireNonNull(nonceStore, "nonceStore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.windowMillis = windowMillis;
        this.headersConfig = Objects.requireNonNull(headersConfig, "headersConfig");
    }

    /**
     * 按配置从 HTTP headers 直接组装入站请求并验签。
     *
     * <p>缺 header、时间戳无法解析等请求级问题统一返回 {@link VerifyResult#INVALID}，
     * 不抛异常、不泄漏具体原因。</p>
     *
     * @param request 原始业务请求数据
     * @param headers 接收到的 HTTP header，按 {@link SignHeaders} 命名
     * @param keys    keyId 到密钥的映射
     * @return 验签结果
     */
    public VerifyResult verify(UnsignedRequest request, Map<String, String> headers,
                               Map<String, SignKey> keys) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(headers, "headers");
        try {
            SignRequest signed = new SignRequest(
                    request.method(), request.path(), request.queryParams(), request.body(),
                    Signs.parseTimestamp(headers.get(headersConfig.timestamp())),
                    headers.get(headersConfig.nonce()),
                    headers.get(headersConfig.keyId()),
                    headers.get(headersConfig.alg()),
                    headers.get(headersConfig.sign()));
            return verify(signed, keys);
        } catch (IllegalArgumentException e) {
            return VerifyResult.INVALID;
        }
    }

    /**
     * 验签并防重放。
     *
     * <p>缺字段、时间戳非正等请求级问题统一返回 {@link VerifyResult#INVALID}，
     * 不抛异常、不泄漏具体原因。</p>
     *
     * @param request 完整入站请求
     * @param keys    keyId 到密钥的映射
     * @return 验签结果
     */
    public VerifyResult verify(SignRequest request, Map<String, SignKey> keys) {
        Objects.requireNonNull(request, "request");
        if (!isSigned(request)) {
            return VerifyResult.INVALID;
        }

        SignKey key = keys == null ? null : keys.get(request.keyId());
        if (key == null) {
            return VerifyResult.INVALID;
        }
        if (!algorithm.name().equals(request.alg())) {
            return VerifyResult.INVALID;
        }
        if (Math.abs(clock.millis() - request.timestamp()) >= windowMillis) {
            return VerifyResult.INVALID;
        }
        if (!algorithm.verify(Signs.canonicalize(request), key.secret(), request.signature())) {
            return VerifyResult.INVALID;
        }
        String nonceKey = request.keyId() + ":" + request.nonce();
        return nonceStore.putIfAbsent(nonceKey, windowMillis)
                ? VerifyResult.SUCCESS : VerifyResult.INVALID;
    }

    /**
     * 判断请求是否带齐验签所需字段。
     *
     * @param request 入站请求
     * @return 字段齐全返回 true
     */
    private boolean isSigned(SignRequest request) {
        return request.timestamp() > 0
                && Strings.isNotBlank(request.nonce())
                && Strings.isNotBlank(request.keyId())
                && Strings.isNotBlank(request.alg())
                && Strings.isNotBlank(request.signature());
    }
}
