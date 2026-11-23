package cn.kennylee.banyan.openapi.sign.client;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 原始 query 串解析：按表单规则解码，同名参数保留多值。
 *
 * <p>HTTP 客户端适配器用它把实际请求 URI 转成签名所需的参数表，避免调用方手工拼装。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class QueryParams {

    private QueryParams() {
    }

    /**
     * 解析原始 query 串。
     *
     * @param rawQuery {@code URI#getRawQuery()} 的结果，可为 null
     * @return 参数名到值列表的只读映射，保持出现顺序
     * @throws IllegalArgumentException 百分号编码非法
     */
    public static Map<String, List<String>> parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            String rawName = index < 0 ? pair : pair.substring(0, index);
            String rawValue = index < 0 ? "" : pair.substring(index + 1);
            String name = decode(rawName);
            if (name.isBlank()) {
                continue;
            }
            params.computeIfAbsent(name, key -> new ArrayList<>()).add(decode(rawValue));
        }
        Map<String, List<String>> copied = new LinkedHashMap<>(params.size());
        params.forEach((key, values) ->
                copied.put(key, Collections.unmodifiableList(new ArrayList<>(values))));
        return Collections.unmodifiableMap(copied);
    }

    /** UTF-8 表单解码。 */
    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(SignMessages.QUERY_ENCODING_INVALID, e);
        }
    }
}
