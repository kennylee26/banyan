package cn.kennylee.banyan.commons.crypto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 已签名、可以发送给第三方的请求：保留原始请求数据，并附带签名参数。
 *
 * <p>通过 {@link #headers()} 获取按 {@link SignHeaders} 约定生成的 HTTP header；
 * 本对象不绑定具体 HTTP 客户端，拿到后用调用方自己的客户端发送即可。</p>
 *
 * @param request       原始请求数据
 * @param authParams    五个协议参数与值
 * @param headersConfig 生成 header 名所用的配置
 * @author kennylee
 * @since 0.1.0
 */
public record SignedRequest(UnsignedRequest request, Map<String, String> authParams,
                            SignHeaders headersConfig) {

    /**
     * 保留参数顺序并提供不可变视图。
     */
    public SignedRequest {
        authParams = Collections.unmodifiableMap(new LinkedHashMap<>(authParams));
        headersConfig = Objects.requireNonNull(headersConfig, "headersConfig");
    }

    /**
     * 兼容两参数构造，使用默认 header 名。
     *
     * @param request    原始请求数据
     * @param authParams 五个协议参数与值
     */
    public SignedRequest(UnsignedRequest request, Map<String, String> authParams) {
        this(request, authParams, SignHeaders.DEFAULT);
    }

    /**
     * 按配置生成需要附加到 HTTP 请求上的 header。
     *
     * @return 五个签名 header，不可修改
     */
    public Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>(5);
        headers.put(headersConfig.alg(), authParams.get(Signs.PARAM_ALG));
        headers.put(headersConfig.keyId(), authParams.get(Signs.PARAM_KEY_ID));
        headers.put(headersConfig.timestamp(), authParams.get(Signs.PARAM_TIMESTAMP));
        headers.put(headersConfig.nonce(), authParams.get(Signs.PARAM_NONCE));
        headers.put(headersConfig.sign(), authParams.get(Signs.PARAM_SIGN));
        return Collections.unmodifiableMap(headers);
    }
}
