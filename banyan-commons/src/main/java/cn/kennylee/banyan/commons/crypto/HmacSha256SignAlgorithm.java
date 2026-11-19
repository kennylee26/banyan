package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.util.Hexs;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * 基于 JDK {@link Mac} 的 HMAC-SHA256 默认实现，输出小写十六进制。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class HmacSha256SignAlgorithm implements SignAlgorithm {

    /** 算法名。 */
    public static final String NAME = "HmacSHA256";

    @Override
    public String name() {
        return NAME;
    }

    /**
     * 计算签名。
     *
     * @param canonical 规范化串
     * @param secret    密钥明文
     * @return 小写十六进制签名
     * @throws IllegalStateException JDK 不提供 HMAC-SHA256（正常不会发生）
     */
    @Override
    public String sign(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance(NAME);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), NAME));
            return Hexs.encode(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is required by the JDK", e);
        }
    }
}
