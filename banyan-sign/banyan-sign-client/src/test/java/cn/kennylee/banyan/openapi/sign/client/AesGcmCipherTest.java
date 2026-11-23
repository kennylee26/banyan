package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AES-256-GCM 加密测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class AesGcmCipherTest {

    /** 加密密钥。 */
    private static final String SECRET = "secret-for-tests";

    private final AesGcmCipher cipher = new AesGcmCipher();

    /** 往返一致。 */
    @Test
    void roundTrips() {
        // when
        String encrypted = cipher.encrypt("{\"id\":1}", SECRET);

        // then
        assertEquals("{\"id\":1}", cipher.decrypt(encrypted, SECRET));
    }

    /** 两次密文不同（随机 IV）。 */
    @Test
    void producesDifferentCiphertext() {
        // when
        String first = cipher.encrypt("{\"id\":1}", SECRET);
        String second = cipher.encrypt("{\"id\":1}", SECRET);

        // then
        assertNotEquals(first, second);
    }

    /** 错误密钥解密失败。 */
    @Test
    void rejectsWrongSecret() {
        // given
        String encrypted = cipher.encrypt("{\"id\":1}", SECRET);

        // when
        assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt(encrypted, "another-secret"));
    }

    /** 篡改密文解密失败。 */
    @Test
    void rejectsTamperedCiphertext() {
        // given
        String encrypted = cipher.encrypt("{\"id\":1}", SECRET);

        // when
        assertThrows(IllegalArgumentException.class,
                () -> cipher.decrypt(encrypted + "AA==", SECRET));
    }

    /** 非法 Base64 解密失败。 */
    @Test
    void rejectsInvalidBase64() {
        // when
        assertThrows(IllegalArgumentException.class, () -> cipher.decrypt("not-base64!", SECRET));
    }
}
