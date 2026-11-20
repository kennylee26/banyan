package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

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
 * 基于 JDK 原生 AES-256-GCM 的请求体加密实现。
 *
 * <p>密文格式固定为 {@code Base64(IV ‖ ciphertext ‖ tag)}：IV 12 字节随机值，
 * tag 128 位。加密密钥由 {@code SHA-256("banyan-aes" ‖ secret)} 派生，
 * 与签名使用的密钥做域分离。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class AesGcmPayloadCipher implements PayloadCipher {

    /** 算法标识。 */
    public static final String NAME = "aes-gcm";

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

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String encrypt(String plaintext, String secret) {
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
            throw new IllegalStateException("AES-256-GCM is required by the JDK", e);
        }
    }

    @Override
    public String decrypt(String ciphertext, String secret) {
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
            byte[] plaintext = cipher.doFinal(payload, IV_BYTES,
                    payload.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw invalid(e);
        }
    }

    /** 派生 32 字节 AES 密钥。 */
    private static byte[] deriveKey(String secret) {
        if (Strings.isBlank(secret)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_KEY_INVALID, "secret"));
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            digest.update(KEY_LABEL.getBytes(StandardCharsets.UTF_8));
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
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

    /** 构造脱敏的解密失败异常。 */
    private static IllegalArgumentException invalid(Throwable cause) {
        return new IllegalArgumentException(
                Messages.get(ErrorCode.PAYLOAD_DECRYPT_FAILED), cause);
    }
}
