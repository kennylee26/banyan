package cn.kennylee.banyan.commons.crypto;

/**
 * 请求体加解密扩展点：默认实现见 {@link AesGcmPayloadCipher}，后续可替换为 SM4 等实现。
 *
 * <p>输入输出都是文本：明文为 UTF-8 字符串，密文为可直接放进请求体的字符串。
 * 实现必须处理 {@code null} 输入（原样返回），并对无法解密的内容抛出参数异常。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface PayloadCipher {

    /**
     * 算法标识，用于门面与配置选择实现。
     *
     * @return 算法标识
     */
    String name();

    /**
     * 加密请求体。
     *
     * @param plaintext 明文，可为 null
     * @param secret    密钥明文
     * @return 密文；输入为 null 时返回 null
     * @throws IllegalArgumentException 密钥为空
     */
    String encrypt(String plaintext, String secret);

    /**
     * 解密请求体。
     *
     * @param ciphertext 密文，可为 null
     * @param secret     密钥明文
     * @return 明文；输入为 null 时返回 null
     * @throws IllegalArgumentException 密钥为空、密文非法或认证失败
     */
    String decrypt(String ciphertext, String secret);
}
