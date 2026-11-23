package cn.kennylee.banyan.openapi.sign.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 签名共享协议：规范化纯函数、摘要与 nonce 生成。
 *
 * <p>规范化串格式固定，改动即破坏与服务端的互操作性：</p>
 * <pre>
 * BANYAN-SIGN-V1
 * {method}
 * {path}
 * {key=value&amp;key=value，key 升序、同名值字典序、空值跳过、UTF-8 URL 编码一次}
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
final class Signs {

    /** 规范化串版本。 */
    static final String VERSION = "BANYAN-SIGN-V1";

    /** 协议算法标识。 */
    static final String ALG = HmacSha256.NAME;

    /** 协议参数名，无论出现在哪里都不参与规范化。 */
    private static final Set<String> PROTOCOL_PARAMS = Set.of(
            "alg", "keyId", "timestamp", "nonce", "sign");

    /** 摘要算法名。 */
    private static final String SHA_256 = "SHA-256";

    /** nonce 字节数。 */
    private static final int NONCE_BYTES = 16;

    /** 安全随机数。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    private Signs() {
    }

    /**
     * 生成规范化串，不修改任何状态。
     *
     * @param request 规范化输入
     * @return 规范化串
     * @throws IllegalArgumentException 必填字段缺失
     */
    static String canonicalize(CanonicalRequest request) {
        require(request.method(), "method");
        require(request.path(), "path");
        require(request.nonce(), "nonce");
        require(request.keyId(), "keyId");
        require(request.alg(), "alg");
        if (request.timestamp() <= 0) {
            throw new IllegalArgumentException(SignMessages.TIMESTAMP_INVALID);
        }
        return VERSION + '\n'
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
     * 请求体的 SHA-256 小写十六进制；null 按空串处理。
     *
     * @param body 请求体
     * @return 摘要
     */
    static String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            String text = body == null ? "" : body;
            return Masks.hex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(SignMessages.SHA256_UNAVAILABLE, e);
        }
    }

    /**
     * 生成一次性随机串：16 字节，小写十六进制。
     *
     * @return nonce
     */
    static String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        RANDOM.nextBytes(bytes);
        return Masks.hex(bytes);
    }

    /** 拼查询串：key 升序、同名值字典序、空值跳过、编码一次。 */
    private static String queryString(Map<String, List<String>> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>(params.size());
        new TreeMap<>(params).forEach((key, values) -> {
            if (PROTOCOL_PARAMS.contains(key) || values == null) {
                return;
            }
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .sorted()
                    .forEach(value -> pairs.add(encode(key) + '=' + encode(value)));
        });
        return String.join("&", pairs);
    }

    /** UTF-8 URL 编码一次。 */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 必填校验，供签名门面复用。 */
    static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(SignMessages.missing(field));
        }
    }
}
