package cn.kennylee.banyan.commons.crypto;

import java.util.Map;

/**
 * 已签名、可以发送给第三方的请求：保留原始请求数据，并附带签名参数。
 *
 * <p>本对象不绑定具体 HTTP 客户端；使用方把 {@link #authParams()} 放进请求的
 * header 或 query，再用自己的客户端发送即可。</p>
 *
 * @param request    原始请求数据
 * @param authParams 附加到 HTTP 请求上的五个签名参数
 * @author kennylee
 * @since 0.1.0
 */
public record SignedRequest(UnsignedRequest request, Map<String, String> authParams) {

    /**
     * 保留参数顺序并提供不可变视图。
     */
    public SignedRequest {
        authParams = java.util.Collections.unmodifiableMap(
                new java.util.LinkedHashMap<>(authParams));
    }
}
