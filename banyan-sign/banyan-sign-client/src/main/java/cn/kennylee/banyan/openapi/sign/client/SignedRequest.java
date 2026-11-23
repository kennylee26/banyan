package cn.kennylee.banyan.openapi.sign.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 签名结果：可直接发送的 header 与请求体。
 *
 * @param headers 五个签名 header，不可修改
 * @param body    加密后的请求体；明文模式或空 body 时为原文
 * @author kennylee
 * @since 0.1.0
 */
public record SignedRequest(Map<String, String> headers, String body) {

    /**
     * 拷贝 header，保证不可修改。
     *
     * @throws NullPointerException headers 为 null
     */
    public SignedRequest {
        headers = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
    }
}
