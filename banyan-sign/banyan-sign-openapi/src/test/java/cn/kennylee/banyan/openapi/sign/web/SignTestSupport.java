package cn.kennylee.banyan.openapi.sign.web;

import cn.kennylee.banyan.commons.crypto.OpenApiSigner;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import org.springframework.http.HttpHeaders;

/**
 * 端到端测试共用：构造签名器与请求头。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class SignTestSupport {

    /** 测试密钥标识。 */
    static final String KEY_ID = "app1";

    /** 测试密钥。 */
    static final String SECRET = "secret";

    private SignTestSupport() {
    }

    /**
     * 构造与测试配置对应的签名器。
     *
     * @param encrypt       是否加密请求体
     * @param headersConfig header 名配置
     * @return 签名器
     */
    static OpenApiSigner signer(boolean encrypt, SignHeaders headersConfig) {
        OpenApiSigner.Builder builder = OpenApiSigner.builder()
                .keyId(KEY_ID)
                .secret(SECRET)
                .headers(headersConfig);
        if (!encrypt) {
            builder.plainBody();
        }
        return builder.build();
    }

    /**
     * 把签名结果转换为请求头。
     *
     * @param payload 签名结果
     * @return 请求头
     */
    static HttpHeaders headers(OpenApiSigner.SignedPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        payload.headers().forEach(headers::add);
        return headers;
    }
}
