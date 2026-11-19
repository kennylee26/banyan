package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

/**
 * 签名密钥：由使用方构造并传入，库不读取任何配置源。
 *
 * @param keyId  密钥标识，参与签名串
 * @param secret 密钥明文，只用于计算签名，不会出现在日志或 toString 中
 * @author kennylee
 * @since 0.1.0
 */
public record SignKey(String keyId, String secret) {

    /** 字段名：密钥标识。 */
    private static final String FIELD_KEY_ID = "keyId";

    /** 字段名：密钥明文。 */
    private static final String FIELD_SECRET = "secret";

    /**
     * 校验并构造密钥。
     *
     * @throws IllegalArgumentException keyId 或 secret 为空
     */
    public SignKey {
        if (Strings.isBlank(keyId)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_KEY_INVALID, FIELD_KEY_ID));
        }
        if (Strings.isBlank(secret)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_KEY_INVALID, FIELD_SECRET));
        }
    }

    /**
     * 脱敏输出，不暴露 secret。
     *
     * @return 描述
     */
    @Override
    public String toString() {
        return "SignKey{keyId='" + keyId + "', secret=<redacted>}";
    }
}
