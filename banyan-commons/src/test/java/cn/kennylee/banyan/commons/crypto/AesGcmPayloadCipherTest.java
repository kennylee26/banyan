package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AES-256-GCM 请求体加解密测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class AesGcmPayloadCipherTest {

    /** 待测实现。 */
    private final PayloadCipher cipher = new AesGcmPayloadCipher();

    /** 密钥明文。 */
    private final String secret = "secret-for-aes-gcm";

    @BeforeEach
    void fixLocale() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 算法标识为 aes-gcm。 */
    @Test
    void exposesAlgorithmName() {
        // when
        assertEquals("aes-gcm", cipher.name());
    }

    /** 加密后可解回明文。 */
    @Test
    void roundTripsPlaintext() {
        // given
        String plaintext = "{\"id\":1,\"name\":\"banyan\"}";

        // when
        String encrypted = cipher.encrypt(plaintext, secret);

        // then
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, cipher.decrypt(encrypted, secret));
    }

    /** 每次加密使用随机 IV，同一明文密文不同。 */
    @Test
    void producesDifferentCiphertextForSamePlaintext() {
        // given
        String plaintext = "{\"id\":1}";

        // when
        String first = cipher.encrypt(plaintext, secret);
        String second = cipher.encrypt(plaintext, secret);

        // then
        assertNotEquals(first, second);
        assertEquals(plaintext, cipher.decrypt(first, secret));
        assertEquals(plaintext, cipher.decrypt(second, secret));
    }

    /** 按文档独立实现可以解出明文，钉死格式与密钥派生。 */
    @Test
    void decryptsWithIndependentImplementation() throws Exception {
        // given
        String plaintext = "{\"id\":1}";
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt(plaintext, secret));
        byte[] iv = Arrays.copyOfRange(raw, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(raw, 12, raw.length);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("banyan-aes".getBytes(StandardCharsets.UTF_8));
        byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        Cipher jdkCipher = Cipher.getInstance("AES/GCM/NoPadding");
        jdkCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, iv));

        // when
        String decrypted = new String(jdkCipher.doFinal(ciphertext), StandardCharsets.UTF_8);

        // then
        assertEquals(plaintext, decrypted);
    }

    /** 密文结构为 IV(12) + 密文 + tag(16)。 */
    @Test
    void encodesIvCiphertextAndTag() {
        // given
        String plaintext = "abc";

        // when
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt(plaintext, secret));

        // then
        assertEquals(12 + plaintext.getBytes(StandardCharsets.UTF_8).length + 16, raw.length);
    }

    /** 篡改任意字节后解密失败。 */
    @Test
    void rejectsTamperedCiphertext() {
        // given
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("{\"id\":1}", secret));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);

        // when
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt(tampered, secret));

        // then
        assertTrue(error.getMessage().contains("解密"));
    }

    /** 错误密钥无法解密。 */
    @Test
    void rejectsWrongSecret() {
        // given
        String encrypted = cipher.encrypt("{\"id\":1}", secret);

        // when
        assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt(encrypted, "another-secret"));
    }

    /** 非法 Base64 被拒绝。 */
    @Test
    void rejectsMalformedBase64() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt("not-base64!!", secret));
    }

    /** 长度不足的密文被拒绝。 */
    @Test
    void rejectsTooShortPayload() {
        // given
        String tooShort = Base64.getEncoder().encodeToString(new byte[27]);

        // when
        assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt(tooShort, secret));
    }

    /** 中文与 emoji 往返一致。 */
    @Test
    void roundTripsUtf8Text() {
        // given
        String plaintext = "订单：榕树 🌳";

        // when
        String encrypted = cipher.encrypt(plaintext, secret);

        // then
        assertEquals(plaintext, cipher.decrypt(encrypted, secret));
    }

    /** 空串可以加密与解密，不会被当作 null。 */
    @Test
    void roundTripsEmptyString() {
        // when
        String encrypted = cipher.encrypt("", secret);

        // then
        assertFalse(encrypted.isEmpty());
        assertEquals("", cipher.decrypt(encrypted, secret));
    }

    /** null 输入原样返回。 */
    @Test
    void keepsNullAsNull() {
        // when
        assertNull(cipher.encrypt(null, secret));
        assertNull(cipher.decrypt(null, secret));
    }

    /** secret 为空时立即失败。 */
    @Test
    void rejectsBlankSecret() {
        // when
        assertThrows(IllegalArgumentException.class, () -> cipher.encrypt("{}", " "));
        assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("AAAA", ""));
    }
}
