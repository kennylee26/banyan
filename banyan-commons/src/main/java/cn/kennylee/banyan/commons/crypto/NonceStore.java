package cn.kennylee.banyan.commons.crypto;

/**
 * 一次性 nonce 存储扩展点：默认实现见 {@link InMemoryNonceStore}，多实例部署由使用方替换。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface NonceStore {

    /**
     * 原子登记 nonce：首次出现返回 true，重复出现返回 false。
     *
     * @param nonceKey  去重键（由调用方拼好，含 keyId）
     * @param ttlMillis 条目存活毫秒数
     * @return 是否首次登记
     */
    boolean putIfAbsent(String nonceKey, long ttlMillis);
}
