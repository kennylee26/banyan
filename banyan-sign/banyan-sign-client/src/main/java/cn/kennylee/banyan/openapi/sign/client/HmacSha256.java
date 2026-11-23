package cn.kennylee.banyan.openapi.sign.client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * 基于 JDK {@link Mac} 的 HMAC-SHA256 实现，输出小写十六进制。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class HmacSha256 {

    /** JDK 算法名。 */
    static final String NAME = "HmacSHA256";

    private HmacSha256() {
    }

    /**
     * 计算签名。
     *
     * @param canonical 规范化串
     * @param secret    密钥明文
     * @return 小写十六进制签名
     * @throws IllegalStateException JDK 不提供 HMAC-SHA256（正常不会发生）
     */
    static String sign(String canonical, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("请求缺少 secret");
        }
        try {
            Mac mac = Mac.getInstance(NAME);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), NAME));
            return Masks.hex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JDK 必须提供 HMAC-SHA256", e);
        }
    }
}
