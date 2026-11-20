package cn.kennylee.banyan.openapi.sign.autoconfigure;

import cn.kennylee.banyan.commons.crypto.AesGcmPayloadCipher;
import cn.kennylee.banyan.commons.crypto.HmacSha256SignAlgorithm;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.Signs;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI 验签与解密配置。所有开关与参数集中在此，避免散读环境变量。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.openapi-sign")
public class OpenApiSignProperties {

    /** 默认错误码。 */
    public static final String DEFAULT_ERROR_CODE = "openapi.sign.invalid";

    /** 模块总开关。 */
    private boolean enabled = true;

    /** 是否要求并执行请求体解密。 */
    private boolean encrypted = true;

    /** 签名算法标识。 */
    private String signAlgorithm = HmacSha256SignAlgorithm.NAME;

    /** 加密算法标识。 */
    private String cipher = AesGcmPayloadCipher.NAME;

    /** 验签时间窗口。 */
    private Duration window = Duration.ofMinutes(5);

    /** 是否在响应中返回具体失败原因，默认关闭。 */
    private boolean revealReason;

    /** 五个签名 header 名。 */
    private Headers headers = new Headers();

    /** keyId 到 secret 的映射，仅用于演示与联调。 */
    private Map<String, String> keys = new LinkedHashMap<>();

    /** 失败响应配置。 */
    private ErrorResponse error = new ErrorResponse();

    /**
     * 转换为签名 header 名配置。
     *
     * @return header 名配置
     * @throws IllegalArgumentException 名称重复或含非法字符
     */
    public SignHeaders toSignHeaders() {
        return new SignHeaders(headers.getAlg(), headers.getKeyId(), headers.getTimestamp(),
                headers.getNonce(), headers.getSign());
    }

    /**
     * 签名 header 名配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Headers {

        /** 算法 header 名。 */
        private String alg = Signs.HEADER_ALG;

        /** 密钥标识 header 名。 */
        private String keyId = Signs.HEADER_KEY_ID;

        /** 时间戳 header 名。 */
        private String timestamp = Signs.HEADER_TIMESTAMP;

        /** nonce header 名。 */
        private String nonce = Signs.HEADER_NONCE;

        /** 签名 header 名。 */
        private String sign = Signs.HEADER_SIGN;
    }

    /**
     * 失败响应配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class ErrorResponse {

        /** 响应中的错误码，不随失败原因变化。 */
        private String code = DEFAULT_ERROR_CODE;

        /** 通用失败文案；为空时取消息资源。 */
        private String message;
    }
}
