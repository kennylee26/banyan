package cn.kennylee.banyan.cache.support;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存 key 拼接：`appName:group:key`，空段省略且不出现连续冒号。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class CacheKeys {

    /** 默认 key 最大长度。 */
    public static final int DEFAULT_MAX_KEY_LENGTH = 512;

    /** 分隔符。 */
    private static final String SEPARATOR = ":";

    /** 应用名，可为空。 */
    private final String appName;

    /** key 最大长度。 */
    private final int maxKeyLength;

    /**
     * 构造实例。
     *
     * @param appName      应用名，可为 null
     * @param maxKeyLength key 最大长度，非正数时使用默认值
     */
    public CacheKeys(String appName, int maxKeyLength) {
        this.appName = Strings.isBlank(appName) ? "" : appName.trim();
        this.maxKeyLength = maxKeyLength > 0 ? maxKeyLength : DEFAULT_MAX_KEY_LENGTH;
    }

    /**
     * 拼接完整 key。
     *
     * @param group 组名（业务前缀），可为空
     * @param key   业务 key
     * @return 完整 key
     * @throws IllegalArgumentException 组名或 key 含分隔符、key 为空、key 超长
     */
    public String build(String group, String key) {
        validatePart(group);
        if (Strings.isBlank(key)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_KEY_INVALID));
        }
        validatePart(key);
        List<String> parts = new ArrayList<>(3);
        addPart(parts, appName);
        addPart(parts, group);
        addPart(parts, key);
        String fullKey = String.join(SEPARATOR, parts);
        if (fullKey.length() > maxKeyLength) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_KEY_TOO_LONG, maxKeyLength));
        }
        return fullKey;
    }

    /**
     * 拼接命名空间前缀，用于 SCAN 批量删除。
     *
     * @param group 组名，可为空
     * @return 以分隔符结尾的前缀
     * @throws IllegalArgumentException 组名含分隔符
     */
    public String namespace(String group) {
        validatePart(group);
        List<String> parts = new ArrayList<>(2);
        addPart(parts, appName);
        addPart(parts, group);
        return parts.isEmpty() ? "" : String.join(SEPARATOR, parts) + SEPARATOR;
    }

    /** 校验单个片段。 */
    private void validatePart(String part) {
        if (part != null && part.contains(SEPARATOR)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_KEY_INVALID));
        }
    }

    /** 追加非空片段。 */
    private void addPart(List<String> parts, String part) {
        if (Strings.isNotBlank(part)) {
            parts.add(part);
        }
    }
}
