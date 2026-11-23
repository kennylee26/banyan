package cn.kennylee.banyan.openapi.sign.support;

import cn.kennylee.banyan.commons.crypto.SignKey;

/**
 * 密钥来源扩展点：使用方声明自己的 bean 时覆盖默认配置实现。
 *
 * <p>实现不得读取请求之外的全局可变状态，也不得把 secret 写进日志。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface SignKeyResolver {

    /**
     * 按 keyId 解析密钥。
     *
     * @param keyId 请求携带的密钥标识
     * @return 密钥；未配置时返回 null
     */
    SignKey resolve(String keyId);
}
