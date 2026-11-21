package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Hexs;
import cn.kennylee.banyan.commons.util.Strings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * 签名共享协议：规范化纯函数、调用方参数组装与时间戳解析。
 *
 * <p>规范化串格式（固定、不可随意改动，改动即破坏互操作性）：</p>
 * <pre>
 * BANYAN-SIGN-V1
 * {method}
 * {path}
 * {key=value&amp;key=value，key 升序、同名值字典序、UTF-8 URL 编码一次}
 * {bodyHash（SHA-256 小写十六进制）}
 * {timestamp（毫秒）}
 * {nonce}
 * {keyId}
 * {alg}
 * </pre>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Signs {

    /** 规范化串版本。 */
    public static final String CANONICAL_VERSION = "BANYAN-SIGN-V1";

    /** 参数名：算法标识。 */
    public static final String PARAM_ALG = "alg";

    /** 参数名：密钥标识。 */
    public static final String PARAM_KEY_ID = "keyId";

    /** 参数名：毫秒时间戳。 */
    public static final String PARAM_TIMESTAMP = "timestamp";

    /** 参数名：一次性随机串。 */
    public static final String PARAM_NONCE = "nonce";

    /** 参数名：签名串。 */
    public static final String PARAM_SIGN = "sign";

    /** HTTP header：算法标识。 */
    public static final String HEADER_ALG = "X-Banyan-Alg";

    /** HTTP header：密钥标识。 */
    public static final String HEADER_KEY_ID = "X-Banyan-KeyId";

    /** HTTP header：毫秒时间戳。 */
    public static final String HEADER_TIMESTAMP = "X-Banyan-Timestamp";

    /** HTTP header：一次性随机串。 */
    public static final String HEADER_NONCE = "X-Banyan-Nonce";

    /** HTTP header：签名串。 */
    public static final String HEADER_SIGN = "X-Banyan-Sign";

    /** 协议参数集合：无论放在 header 还是 query，都不参与规范化。 */
    private static final Set<String> PROTOCOL_PARAMS = Set.of(
            PARAM_ALG, PARAM_KEY_ID, PARAM_TIMESTAMP, PARAM_NONCE, PARAM_SIGN);

    /** 默认算法。 */
    private static final SignAlgorithm DEFAULT_ALGORITHM = new HmacSha256SignAlgorithm();

    /** 安全随机数。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** nonce 字节数。 */
    private static final int NONCE_BYTES = 16;

    /** 摘要算法名。 */
    private static final String SHA_256 = "SHA-256";

    private Signs() {
    }

    /**
     * 生成规范化串，不修改任何状态。
     *
     * @param request 已包含 timestamp / nonce / keyId / alg 的请求
     * @return 规范化串
     * @throws IllegalArgumentException 必填字段缺失
     */
    public static String canonicalize(SignRequest request) {
        Objects.requireNonNull(request, "request");
        requireProtocolFields(request);
        return CANONICAL_VERSION + '\n'
                + request.method() + '\n'
                + request.path() + '\n'
                + queryString(request.queryParams()) + '\n'
                + sha256(request.body()) + '\n'
                + request.timestamp() + '\n'
                + request.nonce() + '\n'
                + request.keyId() + '\n'
                + request.alg();
    }

    /**
     * 为调用方生成签名。常规使用优先选择 {@link RequestSigner}；本方法用于只需要签名参数的场景。
     *
     * @param request 待签名请求
     * @param key     密钥
     * @return 五个附加签名参数，按协议固定顺序的不可变 Map
     */
    public static Map<String, String> sign(UnsignedRequest request, SignKey key) {
        return sign(request, key, DEFAULT_ALGORITHM, Clock.systemUTC());
    }

    /**
     * 用指定算法为调用方生成签名。
     *
     * @param request   待签名请求
     * @param key       密钥
     * @param algorithm 签名算法
     * @return 五个附加签名参数
     */
    public static Map<String, String> sign(UnsignedRequest request, SignKey key,
                                           SignAlgorithm algorithm) {
        return new RequestSigner(algorithm).sign(request, key).authParams();
    }

    /**
     * 用指定算法与时钟为调用方生成签名。
     *
     * @param request   待签名请求
     * @param key       密钥
     * @param algorithm 签名算法
     * @param clock     时钟
     * @return 五个附加签名参数
     */
    public static Map<String, String> sign(UnsignedRequest request, SignKey key,
                                           SignAlgorithm algorithm, Clock clock) {
        return new RequestSigner(algorithm, clock).sign(request, key).authParams();
    }

    /** 按协议固定顺序组装签名参数。 */
    static Map<String, String> authParams(String alg, String keyId, long timestamp,
                                          String nonce, String signature) {
        Map<String, String> params = new LinkedHashMap<>(5);
        params.put(PARAM_ALG, alg);
        params.put(PARAM_KEY_ID, keyId);
        params.put(PARAM_TIMESTAMP, Long.toString(timestamp));
        params.put(PARAM_NONCE, nonce);
        params.put(PARAM_SIGN, signature);
        return Collections.unmodifiableMap(params);
    }

    /**
     * 解析毫秒时间戳，把数字格式错误转换为带错误码的参数异常。
     *
     * @param value 时间戳文本
     * @return 毫秒时间戳
     * @throws IllegalArgumentException 不是合法数字
     */
    public static long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, PARAM_TIMESTAMP), e);
        }
    }

    /** 生成一次性随机串（16 字节，小写十六进制）。 */
    static String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        RANDOM.nextBytes(bytes);
        return Hexs.encode(bytes);
    }

    /**
     * 判断请求是否带齐验签所需字段（协议字段加签名串）。
     *
     * <p>与 {@link #requireProtocolFields(SignRequest)} 共用同一套规则，
     * 验签入口据此把缺字段统一归为格式非法。</p>
     *
     * @param request 请求
     * @return 字段齐全返回 true
     */
    static boolean hasSignatureFields(SignRequest request) {
        if (Strings.isBlank(request.signature())) {
            return false;
        }
        try {
            requireProtocolFields(request);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 校验规范化/验签共用的协议字段，供同包内的验签复用。
     *
     * @param request 请求
     * @throws IllegalArgumentException timestamp 非正或 nonce/keyId/alg 为空
     */
    static void requireProtocolFields(SignRequest request) {
        if (request.timestamp() <= 0) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, PARAM_TIMESTAMP));
        }
        SignRequest.requireText(request.nonce(), PARAM_NONCE);
        SignRequest.requireText(request.keyId(), PARAM_KEY_ID);
        SignRequest.requireText(request.alg(), PARAM_ALG);
    }

    /** 拼查询串：key 升序、同名值字典序、空值跳过、编码一次。 */
    private static String queryString(Map<String, List<String>> params) {
        List<String> pairs = new ArrayList<>(params.size());
        new TreeMap<>(params).forEach((key, values) -> {
            if (PROTOCOL_PARAMS.contains(key)) {
                return;
            }
            values.stream()
                    .filter(Strings::isNotBlank)
                    .sorted()
                    .forEach(value -> pairs.add(encode(key) + '=' + encode(value)));
        });
        return String.join("&", pairs);
    }

    /** UTF-8 URL 编码一次。 */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 请求体的 SHA-256 小写十六进制。 */
    private static String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            String text = body == null ? "" : body;
            return Hexs.encode(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }
}
