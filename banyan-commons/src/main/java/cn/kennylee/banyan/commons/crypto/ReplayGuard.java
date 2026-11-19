package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

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

    /**
     * 使用默认窗口与系统时钟构造实例。
     *
     * @param algorithm  签名算法
     * @param nonceStore nonce 存储
     */
    public ReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore) {
        this(algorithm, nonceStore, Clock.systemUTC(), DEFAULT_WINDOW_MILLIS);
    }

    /**
     * 构造实例。
     *
     * @param algorithm    签名算法
     * @param nonceStore   nonce 存储
     * @param clock        时钟
     * @param windowMillis 时间窗口毫秒数，必须为正
     * @throws IllegalArgumentException 窗口非正
     */
    public ReplayGuard(SignAlgorithm algorithm, NonceStore nonceStore, Clock clock,
                       long windowMillis) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, FIELD_WINDOW));
        }
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.nonceStore = Objects.requireNonNull(nonceStore, "nonceStore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.windowMillis = windowMillis;
    }

    /**
     * 验签并防重放。
     *
     * @param request 完整入站请求
     * @param keys    keyId 到密钥的映射
     * @return 验签结果
     * @throws IllegalArgumentException 请求格式非法（缺字段等）
     */
    public VerifyResult verifyInbound(SignRequest request, Map<String, SignKey> keys) {
        Objects.requireNonNull(request, "request");
        requireSigned(request);

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

    /** 校验入站请求必填字段。 */
    private void requireSigned(SignRequest request) {
        Signs.requireProtocolFields(request);
        SignRequest.requireText(request.signature(), Signs.PARAM_SIGN);
    }
}
