package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待签名/待验签请求的纯数据载体：只含字符串、Map 与基本类型，
 * 不含任何 HTTP / Spring 类型。
 *
 * <p>出站场景用 {@link #outbound(String, String, Map, String)} 创建，时间戳、nonce、
 * keyId、alg、signature 由 {@link Signs#signOutbound} 填充；入站场景由使用方从自己的
 * HTTP 层解析后构造完整对象。</p>
 *
 * @param method      请求方法
 * @param path        请求路径
 * @param queryParams 查询参数，允许同名多值
 * @param body        请求体原文，可为 null（按空串参与摘要）
 * @param timestamp   毫秒时间戳，出站创建时为 0
 * @param nonce       一次性随机串，出站创建时为 null
 * @param keyId       密钥标识，出站创建时为 null
 * @param alg         签名算法标识，出站创建时为 null
 * @param signature   签名串，出站创建时为 null
 * @author kennylee
 * @since 0.1.0
 */
public record SignRequest(String method, String path, Map<String, List<String>> queryParams,
                          String body, long timestamp, String nonce, String keyId,
                          String alg, String signature) {

    /** 字段名：请求方法。 */
    private static final String FIELD_METHOD = "method";

    /** 字段名：请求路径。 */
    private static final String FIELD_PATH = "path";

    /**
     * 校验必填字段并拷贝查询参数。
     *
     * @throws IllegalArgumentException method 或 path 为空
     */
    public SignRequest {
        requireText(method, FIELD_METHOD);
        requireText(path, FIELD_PATH);
        queryParams = copyParams(queryParams);
    }

    /**
     * 创建出站请求：只有 method / path / query / body，其余字段待签名时填充。
     *
     * @param method      请求方法
     * @param path        请求路径
     * @param queryParams 查询参数
     * @param body        请求体
     * @return 出站请求
     */
    public static SignRequest outbound(String method, String path,
                                       Map<String, List<String>> queryParams, String body) {
        return new SignRequest(method, path, queryParams, body, 0L, null, null, null, null);
    }

    /**
     * 脱敏输出：不暴露 body 与签名串。
     *
     * @return 描述
     */
    @Override
    public String toString() {
        return "SignRequest{method='" + method + "', path='" + path + "', queryKeys="
                + queryParams.keySet() + ", body=<redacted>, timestamp=" + timestamp
                + ", nonce='" + nonce + "', keyId='" + keyId + "', alg='" + alg
                + "', signature=<redacted>}";
    }

    /**
     * 必填校验，供同包内的签名/验签复用。
     *
     * @param value 待校验值
     * @param field 字段名
     * @throws IllegalArgumentException 值为空
     */
    static void requireText(String value, String field) {
        if (Strings.isBlank(value)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, field));
        }
    }

    /** 深拷贝查询参数并过滤 null。 */
    private static Map<String, List<String>> copyParams(Map<String, List<String>> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copied = new LinkedHashMap<>(params.size());
        params.forEach((key, values) -> {
            if (Strings.isBlank(key) || values == null) {
                return;
            }
            List<String> nonNull = new ArrayList<>(values.size());
            values.stream().filter(value -> value != null).forEach(nonNull::add);
            copied.put(key, Collections.unmodifiableList(nonNull));
        });
        return Collections.unmodifiableMap(copied);
    }
}
