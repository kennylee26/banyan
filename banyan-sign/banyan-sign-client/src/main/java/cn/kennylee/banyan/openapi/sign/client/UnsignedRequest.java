package cn.kennylee.banyan.openapi.sign.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待签名请求：调用方只需提供业务请求本身，不需要理解协议参数。
 *
 * @param method      HTTP 方法
 * @param path        请求路径
 * @param queryParams 查询参数，允许同名多值
 * @param body        请求体原文，可为 null
 * @author kennylee
 * @since 0.1.0
 */
public record UnsignedRequest(String method, String path,
                              Map<String, List<String>> queryParams, String body) {

    /**
     * 校验必填字段并深拷贝查询参数。
     *
     * @throws IllegalArgumentException method 或 path 为空
     */
    public UnsignedRequest {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("请求缺少 method");
        }
        if (path == null) {
            throw new IllegalArgumentException("请求缺少 path");
        }
        // URI 没有 path 时 getRawPath() 返回空串；服务端根路径签的是 "/"
        path = path.isEmpty() ? "/" : path;
        if (path.isBlank()) {
            throw new IllegalArgumentException("请求缺少 path");
        }
        queryParams = copyParams(queryParams);
    }

    /** 深拷贝查询参数，过滤 null，保持不可修改。 */
    private static Map<String, List<String>> copyParams(Map<String, List<String>> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copied = new LinkedHashMap<>(params.size());
        params.forEach((key, values) -> {
            if (key == null || key.isBlank() || values == null) {
                return;
            }
            List<String> nonNullValues = new ArrayList<>(values.size());
            values.stream().filter(value -> value != null).forEach(nonNullValues::add);
            copied.put(key, Collections.unmodifiableList(nonNullValues));
        });
        return Collections.unmodifiableMap(copied);
    }
}
