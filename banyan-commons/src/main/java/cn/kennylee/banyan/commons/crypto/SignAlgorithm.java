package cn.kennylee.banyan.commons.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 签名算法扩展点：默认实现见 {@link HmacSha256SignAlgorithm}，使用方可自行替换。
 *
 * <p>默认 {@link #verify} 先重算签名再用 {@link MessageDigest#isEqual} 做常量时间比较，
 * 实现方覆写时 MUST 保持常量时间语义，不得改用 {@code String.equals}。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface SignAlgorithm {

    /**
     * 算法标识，写入签名串的 {@code alg} 参数。
     *
     * @return 算法名
     */
    String name();

    /**
     * 计算签名。
     *
     * @param canonical 规范化串
     * @param secret    密钥明文
     * @return 十六进制小写签名
     */
    String sign(String canonical, String secret);

    /**
     * 常量时间验签。
     *
     * @param canonical 规范化串
     * @param secret    密钥明文
     * @param signature 待校验签名
     * @return 是否匹配
     */
    default boolean verify(String canonical, String secret, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = sign(canonical, secret).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
