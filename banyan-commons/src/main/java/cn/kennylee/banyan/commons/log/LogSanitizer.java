package cn.kennylee.banyan.commons.log;

import cn.kennylee.banyan.commons.util.CollUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 请求/响应日志格式化：忽略头、敏感值脱敏与 body 截断。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class LogSanitizer {

    /** 敏感值占位。 */
    private static final String MASKED = "***";

    /** 截断标记。 */
    private static final String TRUNCATED = "...<truncated>";

    private LogSanitizer() {
    }

    /**
     * 格式化 header 表，按名排序便于比对。
     *
     * @param headers header 表
     * @param options 日志配置
     * @return 可打印文本
     */
    public static String headers(Map<String, String> headers, LoggingOptions options) {
        Set<String> ignored = CollUtils.lowerSet(options.getIgnoredHeaders());
        Set<String> masked = CollUtils.lowerSet(options.getMaskedHeaders());
        Map<String, String> sorted = new TreeMap<>();
        headers.forEach((name, value) -> {
            if (ignored.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            String output = options.isMaskSensitive()
                    && masked.contains(name.toLowerCase(Locale.ROOT)) ? MASKED : value;
            sorted.put(name, output);
        });
        return sorted.toString();
    }

    /**
     * 格式化 body，超长时截断。
     *
     * @param body    body 文本
     * @param options 日志配置
     * @return 可打印文本
     */
    public static String body(String body, LoggingOptions options) {
        String text = body == null ? "" : body;
        int limit = Math.max(0, options.getMaxBodyLength());
        return text.length() <= limit ? text
                : text.substring(0, limit) + TRUNCATED;
    }

    /**
     * 按 UTF-8 把 body 字节转为文本。
     *
     * @param bytes body 字节
     * @return 文本
     */
    public static String utf8(byte[] bytes) {
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }
}
