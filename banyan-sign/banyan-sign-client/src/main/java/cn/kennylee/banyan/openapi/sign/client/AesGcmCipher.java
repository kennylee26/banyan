package cn.kennylee.banyan.openapi.sign.client;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 请求体加解密：密文格式为 {@code Base64(IV ‖ ciphertext ‖ tag)}。
 *
 * <p>IV 12 字节随机值，tag 128 位；加密密钥由 {@code SHA-256("banyan-aes" ‖ secret)} 派生。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
final class AesGcmCipher {

    /** JDK 变换名。 */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** JDK 密钥算法名。 */
    private static final String KEY_ALGORITHM = "AES";

    /** 摘要算法名。 */
    private static final String SHA_256 = "SHA-256";

    /** 密钥派生标签，与 HMAC 密钥做域分离。 */
    private static final String KEY_LABEL = "banyan-aes";

    /** IV 字节数。 */
    private static final int IV_BYTES = 12;

    /** tag 位数。 */
    private static final int TAG_BITS = 128;

    /** tag 字节数。 */
    private static final int TAG_BYTES = TAG_BITS / 8;

    /** 密文最小长度：IV + tag。 */
    private static final int MIN_CIPHERTEXT_BYTES = IV_BYTES + TAG_BYTES;

    /** 安全随机数。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 加密请求体；null 原样返回。
     *
     * @param plaintext 明文
     * @param secret    密钥明文
     * @return Base64 密文
     */
    String encrypt(String plaintext, String secret) {
        if (plaintext == null) {
            return null;
        }
        byte[] key = deriveKey(secret);
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[IV_BYTES + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, IV_BYTES);
            System.arraycopy(encrypted, 0, payload, IV_BYTES, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JDK 必须提供 AES-256-GCM", e);
        }
    }

    /**
     * 解密请求体；仅用于测试与排查，第三方正常不需要调用。
     *
     * @param ciphertext Base64 密文
     * @param secret     密钥明文
     * @return 明文
     */
    String decrypt(String ciphertext, String secret) {
        if (ciphertext == null) {
            return null;
        }
        byte[] key = deriveKey(secret);
        byte[] payload = decode(ciphertext);
        if (payload.length < MIN_CIPHERTEXT_BYTES) {
            throw invalid(null);
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            byte[] plaintext = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw invalid(e);
        }
    }

    /** 派生 32 字节 AES 密钥。 */
    private static byte[] deriveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("请求缺少 secret");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            digest.update(KEY_LABEL.getBytes(StandardCharsets.UTF_8));
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 必须提供 SHA-256", e);
        }
    }

    /** 解析 Base64，失败时统一转换为解密失败。 */
    private static byte[] decode(String ciphertext) {
        try {
            return Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException e) {
            throw invalid(e);
        }
    }

    /** 构造不暴露细节的解密失败异常。 */
    private static IllegalArgumentException invalid(Throwable cause) {
        return new IllegalArgumentException("请求体解密失败", cause);
    }
}
