package cn.kennylee.banyan.openapi.sign.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 第三方 OpenAPI 签名门面：一次调用生成可发送的 header 与请求体。
 *
 * <p>默认先加密再签名（encrypt-then-sign）；{@link Builder#plainBody()} 可切换为只签名原文。
 * 只准备数据，不发送 HTTP 请求。密钥与配置均为实例级，可在不同调用方之间隔离。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class OpenApiRequestSigner {

    /** 日志。 */
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiRequestSigner.class);

    /** 密钥标识。 */
    private final String keyId;

    /** 密钥明文。 */
    private final String secret;

    /** header 名配置。 */
    private final SignHeaders headersConfig;

    /** 需要从请求路径剥离的前缀，空表示不剥离。 */
    private final String basePath;

    /** 是否加密请求体。 */
    private final boolean encrypted;

    /** 时钟。 */
    private final Clock clock;

    /** 加密实现。 */
    private final AesGcmCipher cipher = new AesGcmCipher();

    private OpenApiRequestSigner(Builder builder) {
        this.keyId = builder.keyId;
        this.secret = builder.secret;
        this.headersConfig = builder.headersConfig;
        this.basePath = builder.basePath;
        this.encrypted = builder.encrypted;
        this.clock = builder.clock;
        LOG.info("OpenAPI 签名客户端初始化: keyId={}, encrypted={}, basePath={}",
                keyId, encrypted, basePath.isEmpty() ? "<none>" : basePath);
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
     * @throws IllegalArgumentException basePath 不匹配或请求缺少必填字段
     */
    public SignedRequest sign(UnsignedRequest request) {
        Objects.requireNonNull(request, "request");
        String path = stripBasePath(request.path());
        int plainLength = request.body() == null ? 0 : request.body().length();
        String body = encryptBody(request.body());
        LOG.trace("OpenAPI 请求体长度: 明文={}, 发送={}", plainLength,
                body == null ? 0 : body.length());
        long timestamp = clock.millis();
        String nonce = Signs.newNonce();
        LOG.debug("OpenAPI 签名请求: method={}, path={}, queryKeys={}, keyId={}, timestamp={}, nonce={}",
                request.method(), path, request.queryParams().keySet(), keyId, timestamp, nonce);

        CanonicalRequest canonicalRequest = new CanonicalRequest(request.method(), path,
                request.queryParams(), body, timestamp, nonce, keyId, Signs.ALG);
        String canonical = Signs.canonicalize(canonicalRequest);
        LOG.trace("OpenAPI 规范化串: {}", canonical);

        String signature = HmacSha256.sign(canonical, secret);
        Map<String, String> headers = new LinkedHashMap<>(5);
        headers.put(headersConfig.alg(), Signs.ALG);
        headers.put(headersConfig.keyId(), keyId);
        headers.put(headersConfig.timestamp(), Long.toString(timestamp));
        headers.put(headersConfig.nonce(), nonce);
        headers.put(headersConfig.sign(), signature);
        LOG.trace("OpenAPI 签名 header: {}", maskedHeaders(headers));
        return new SignedRequest(headers, body);
    }

    /**
     * 脱敏输出，不包含 secret。
     *
     * @return 描述
     */
    @Override
    public String toString() {
        return "OpenApiRequestSigner{keyId='" + keyId + "', encrypted=" + encrypted
                + ", basePath='" + basePath + "', secret=<redacted>}";
    }

    /** 剥离 basePath；不匹配时立即失败。 */
    private String stripBasePath(String path) {
        if (basePath.isEmpty()) {
            return path;
        }
        if (path.equals(basePath)) {
            return "/";
        }
        if (!path.startsWith(basePath + "/")) {
            LOG.warn("OpenAPI 签名路径不匹配 basePath: basePath={}, path={}", basePath, path);
            throw new IllegalArgumentException("请求路径不包含 basePath: " + basePath);
        }
        return path.substring(basePath.length());
    }

    /** 按模式加密请求体；空 body 不加密。 */
    private String encryptBody(String body) {
        if (!encrypted || body == null || body.isEmpty()) {
            return body;
        }
        try {
            return cipher.encrypt(body, secret);
        } catch (RuntimeException e) {
            LOG.error("OpenAPI 请求体加密失败: keyId={}", keyId, e);
            throw e;
        }
    }

    /** 复制 header 并把签名替换为前 8 位，供 TRACE 日志使用。 */
    private Map<String, String> maskedHeaders(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>(headers);
        masked.put(headersConfig.sign(),
                Masks.first8(headers.get(headersConfig.sign())));
        return masked;
    }

    /**
     * 门面构建器：密钥、header 名、basePath 与加密模式。
     *
     * @author kennylee
     * @since 0.1.0
     */
    public static final class Builder {

        /** 密钥标识。 */
        private String keyId;

        /** 密钥明文。 */
        private String secret;

        /** header 名配置。 */
        private SignHeaders headersConfig = SignHeaders.DEFAULT;

        /** 路径前缀。 */
        private String basePath = "";

        /** 是否加密。 */
        private boolean encrypted = true;

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
         * 设置 header 名，须与提供方一致。
         *
         * @param value header 名配置
         * @return 当前构建器
         */
        public Builder headers(SignHeaders value) {
            this.headersConfig = Objects.requireNonNull(value, "headers");
            return this;
        }

        /**
         * 设置需要从请求路径剥离的前缀。
         *
         * @param value 例如 {@code /api}；默认空
         * @return 当前构建器
         */
        public Builder basePath(String value) {
            this.basePath = value == null ? "" : value;
            return this;
        }

        /**
         * 切换为只签名原文，不加密。
         *
         * @return 当前构建器
         */
        public Builder plainBody() {
            this.encrypted = false;
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
         * @throws IllegalArgumentException 密钥缺失或 basePath 不合法
         */
        public OpenApiRequestSigner build() {
            Signs.require(keyId, "keyId");
            Signs.require(secret, "secret");
            if (!basePath.isEmpty()
                    && (!basePath.startsWith("/") || basePath.endsWith("/"))) {
                throw new IllegalArgumentException("basePath 必须以 / 开头且不以 / 结尾");
            }
            return new OpenApiRequestSigner(this);
        }
    }
}
