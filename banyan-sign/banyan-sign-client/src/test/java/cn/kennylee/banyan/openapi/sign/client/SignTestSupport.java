package cn.kennylee.banyan.openapi.sign.client;

import java.util.List;
import java.util.Map;

/**
 * 适配器测试共用的协议工具：规范化、摘要与 HMAC。
 *
 * <p>仅用于测试，随 test-jar 提供给 HttpClient5 / RestTemplate / JDK 适配器。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SignTestSupport {

    /** 协议算法标识。 */
    public static final String ALG = HmacSha256.NAME;

    private SignTestSupport() {
    }

    /**
     * 规范化串。
     *
     * @param method    HTTP 方法
     * @param path      路径
     * @param query     查询参数
     * @param body      实际发送的 body
     * @param timestamp 毫秒时间戳
     * @param nonce     nonce
     * @param keyId     密钥标识
     * @return 规范化串
     */
    public static String canonical(String method, String path, Map<String, List<String>> query,
                                   String body, long timestamp, String nonce, String keyId) {
        return Signs.canonicalize(new CanonicalRequest(method, path, query, body,
                timestamp, nonce, keyId, ALG));
    }

    /**
     * SHA-256 小写十六进制。
     *
     * @param text 文本
     * @return 摘要
     */
    public static String sha256(String text) {
        return Signs.sha256(text);
    }

    /**
     * HMAC-SHA256 小写十六进制。
     *
     * @param canonical 规范化串
     * @param secret    密钥
     * @return 签名
     */
    public static String hmac(String canonical, String secret) {
        return HmacSha256.sign(canonical, secret);
    }
}
