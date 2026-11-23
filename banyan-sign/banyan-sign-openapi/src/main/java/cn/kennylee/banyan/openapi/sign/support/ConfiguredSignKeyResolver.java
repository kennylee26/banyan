package cn.kennylee.banyan.openapi.sign.support;

import cn.kennylee.banyan.commons.crypto.SignKey;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从配置读取密钥的默认实现：只适合演示与联调，生产建议接入配置中心或密钥服务。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class ConfiguredSignKeyResolver implements SignKeyResolver {

    /** keyId 到 secret 的不可变映射。 */
    private final Map<String, String> keys;

    /**
     * 构造实例并校验配置。
     *
     * @param keys keyId 到 secret 的映射，可为 null
     * @throws IllegalArgumentException keyId 或 secret 为空
     */
    public ConfiguredSignKeyResolver(Map<String, String> keys) {
        Map<String, String> copied = new LinkedHashMap<>();
        if (keys != null) {
            keys.forEach((keyId, secret) -> {
                if (Strings.isBlank(keyId)) {
                    throw new IllegalArgumentException(
                            Messages.get(ErrorCode.SIGN_KEY_INVALID, "keyId"));
                }
                if (Strings.isBlank(secret)) {
                    throw new IllegalArgumentException(
                            Messages.get(ErrorCode.SIGN_KEY_INVALID, "secret"));
                }
                copied.put(keyId, secret);
            });
        }
        this.keys = Collections.unmodifiableMap(copied);
    }

    @Override
    public SignKey resolve(String keyId) {
        if (Strings.isBlank(keyId)) {
            return null;
        }
        String secret = keys.get(keyId);
        return secret == null ? null : new SignKey(keyId, secret);
    }
}
