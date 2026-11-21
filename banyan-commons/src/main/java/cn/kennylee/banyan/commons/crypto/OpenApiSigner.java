package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 面向客户的 OpenAPI 签名门面：一次调用返回可直接发送的签名 header 与请求体。
 *
 * <p>默认先加密再签名（encrypt-then-sign）：签名覆盖密文，明文不参与规范化。
 * 只准备数据，不发送 HTTP 请求。</p>
 *
 * <pre>{@code
 * OpenApiSigner signer = OpenApiSigner.builder()
 *         .keyId("app1")
 *         .secret("secret")
 *         .build();
 *
 * OpenApiSigner.SignedPayload payload = signer.sign(request);
 * payload.headers();  // 五个签名 header
 * payload.body();     // 密文，直接作为请求体发送
 * }</pre>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class OpenApiSigner {

    /** 参数名：签名算法。 */
    private static final String FIELD_ALGORITHM = "algorithm";

    /** 参数名：加密算法。 */
    private static final String FIELD_CIPHER = "cipher";

    /** 密钥。 */
    private final SignKey key;

    /** 签名算法。 */
    private final SignAlgorithm algorithm;

    /** 加密实现；关闭加密时为 null。 */
    private final PayloadCipher cipher;

    /** 复用调用方签名器，避免重复实现协议组装。 */
    private final RequestSigner requestSigner;

    /**
     * 构造门面。
     *
     * @param key           密钥
     * @param algorithm     签名算法
     * @param headersConfig header 名配置
     * @param cipher        加密实现，关闭加密时为 null
     * @param clock         时钟
     */
    private OpenApiSigner(SignKey key, SignAlgorithm algorithm, SignHeaders headersConfig,
                          PayloadCipher cipher, Clock clock) {
        this.key = key;
        this.algorithm = algorithm;
        this.cipher = cipher;
        this.requestSigner = new RequestSigner(algorithm, clock, headersConfig);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 为请求生成签名 header 与请求体。
     *
     * @param request 未签名请求
     * @return 可直接发送的 header 与 body
     * @throws IllegalArgumentException 请求缺少 method/path
     */
    public SignedPayload sign(UnsignedRequest request) {
        Objects.requireNonNull(request, "request");
        String body = encryptBody(request.body());
        UnsignedRequest encrypted = new UnsignedRequest(request.method(), request.path(),
                request.queryParams(), body);
        SignedRequest signed = requestSigner.sign(encrypted, key);
        return new SignedPayload(signed.headers(), body);
    }

    /**
     * 脱敏输出，不包含 secret。
     *
     * @return 描述
     */
    @Override
    public String toString() {
        return "OpenApiSigner{keyId='" + key.keyId() + "', algorithm='" + algorithm.name()
                + "', cipher=" + (cipher == null ? "none" : cipher.name()) + "}";
    }

    /** 加密请求体；空 body 与关闭加密时原样返回。 */
    private String encryptBody(String plaintext) {
        if (cipher == null || plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        return cipher.encrypt(plaintext, key.secret());
    }

    /**
     * 已签名请求：header 与可直接发送的 body。
     *
     * @param headers 五个签名 header，不可修改
     * @param body    加密后的请求体；未加密或空 body 时为原文
     * @author kennylee
     * @since 0.1.0
     */
    public record SignedPayload(Map<String, String> headers, String body) {

        /**
         * 拷贝 header，保证不可修改。
         *
         * @throws NullPointerException headers 为 null
         */
        public SignedPayload {
            headers = Collections.unmodifiableMap(
                    new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        }
    }

    /**
     * 门面构建器：聚合密钥、签名算法标识、加密实现与 header 名配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    public static final class Builder {

        /** 密钥标识。 */
        private String keyId;

        /** 密钥明文。 */
        private String secret;

        /** 签名算法标识。 */
        private String algorithm = HmacSha256SignAlgorithm.NAME;

        /** header 名配置。 */
        private SignHeaders headersConfig = SignHeaders.DEFAULT;

        /** 加密实现。 */
        private PayloadCipher cipher = new AesGcmPayloadCipher();

        /** 时钟。 */
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * 设置密钥标识。
         *
         * @param value 密钥标识
         * @return 当前构建器
         */
        public Builder keyId(String value) {
            this.keyId = value;
            return this;
        }

        /**
         * 设置密钥明文。
         *
         * @param value 密钥明文
         * @return 当前构建器
         */
        public Builder secret(String value) {
            this.secret = value;
            return this;
        }

        /**
         * 设置签名算法标识。
         *
         * @param value 算法标识，当前只支持 HMAC-SHA256
         * @return 当前构建器
         */
        public Builder algorithm(String value) {
            this.algorithm = value;
            return this;
        }

        /**
         * 设置 header 名配置。
         *
         * @param value header 名配置，须与提供方一致
         * @return 当前构建器
         */
        public Builder headers(SignHeaders value) {
            this.headersConfig = Objects.requireNonNull(value, "headers");
            return this;
        }

        /**
         * 使用自定义加密实现。
         *
         * @param value 加密实现
         * @return 当前构建器
         */
        public Builder cipher(PayloadCipher value) {
            this.cipher = Objects.requireNonNull(value, "cipher");
            return this;
        }

        /**
         * 按标识选择内置加密实现。
         *
         * @param identifier 加密算法标识
         * @return 当前构建器
         * @throws IllegalArgumentException 标识未支持
         */
        public Builder cipher(String identifier) {
            requireSupported(FIELD_CIPHER, AesGcmPayloadCipher.NAME, identifier);
            this.cipher = new AesGcmPayloadCipher();
            return this;
        }

        /**
         * 关闭加密，只签名明文。
         *
         * @return 当前构建器
         */
        public Builder plainBody() {
            this.cipher = null;
            return this;
        }

        /**
         * 注入时钟，仅供测试固定时间。
         *
         * @param value 时钟
         * @return 当前构建器
         */
        Builder clock(Clock value) {
            this.clock = Objects.requireNonNull(value, "clock");
            return this;
        }

        /**
         * 构建门面。
         *
         * @return 签名门面
         * @throws IllegalArgumentException 密钥缺失、算法不支持
         */
        public OpenApiSigner build() {
            SignKey key = new SignKey(keyId, secret);
            SignAlgorithm signAlgorithm = resolveAlgorithm();
            return new OpenApiSigner(key, signAlgorithm, headersConfig, cipher, clock);
        }

        /** 解析签名算法标识。 */
        private SignAlgorithm resolveAlgorithm() {
            requireSupported(FIELD_ALGORITHM, HmacSha256SignAlgorithm.NAME, algorithm);
            return new HmacSha256SignAlgorithm();
        }

        /** 校验标识受支持，否则抛带字段名的参数异常。 */
        private static void requireSupported(String field, String expected, String actual) {
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                        Messages.get(ErrorCode.SIGN_REQUEST_INVALID, field));
            }
        }
    }
}
