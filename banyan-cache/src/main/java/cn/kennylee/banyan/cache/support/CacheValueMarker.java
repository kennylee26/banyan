package cn.kennylee.banyan.cache.support;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

/**
 * 缓存值类型标记：区分本库写入的 JSON 值与原始字符串，防止混用后静默解析错位。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class CacheValueMarker {

    /** JSON 值前缀。 */
    private static final String JSON_PREFIX = "BANYAN:JSON:";

    /** 原始字符串前缀。 */
    private static final String RAW_PREFIX = "BANYAN:RAW:";

    private CacheValueMarker() {
    }

    /**
     * 编码 JSON 值。
     *
     * @param json     JSON 文本
     * @param enabled 是否启用标记
     * @return 待写入的值
     */
    public static String json(String json, boolean enabled) {
        return enabled ? JSON_PREFIX + json : json;
    }

    /**
     * 编码原始字符串。
     *
     * @param raw     原始字符串
     * @param enabled 是否启用标记
     * @return 待写入的值
     */
    public static String raw(String raw, boolean enabled) {
        return enabled ? RAW_PREFIX + raw : raw;
    }

    /**
     * 校验并取出 JSON 文本。
     *
     * @param stored  已存储的值
     * @param enabled 是否启用标记
     * @return JSON 文本
     * @throws IllegalArgumentException 值不是本库写入的 JSON
     */
    public static String requireJson(String stored, boolean enabled) {
        if (!enabled) {
            return stored;
        }
        if (!stored.startsWith(JSON_PREFIX)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_VALUE_TYPE_MISMATCH, "JSON"));
        }
        return stored.substring(JSON_PREFIX.length());
    }

    /**
     * 校验并取出原始字符串。
     *
     * @param stored  已存储的值
     * @param enabled 是否启用标记
     * @return 原始字符串
     * @throws IllegalArgumentException 值不是本库写入的原始字符串
     */
    public static String requireRaw(String stored, boolean enabled) {
        if (!enabled) {
            return stored;
        }
        if (!stored.startsWith(RAW_PREFIX)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_VALUE_TYPE_MISMATCH, "RAW"));
        }
        return stored.substring(RAW_PREFIX.length());
    }
}
