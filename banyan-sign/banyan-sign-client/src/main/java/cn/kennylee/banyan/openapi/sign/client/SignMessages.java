package cn.kennylee.banyan.openapi.sign.client;

/**
 * 客户端错误文案：集中管理，第三方需要本地化时可替换为同义文案。
 *
 * <p>客户端不依赖 banyan-commons，因此不使用 {@code ErrorCode + Messages}；
 * 这是换取可拷贝、零 banyan 依赖的有意取舍。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SignMessages {

    /** 缺少 method。 */
    public static final String METHOD_MISSING = "请求缺少 method";

    /** 缺少 path。 */
    public static final String PATH_MISSING = "请求缺少 path";

    /** 时间戳非法。 */
    public static final String TIMESTAMP_INVALID = "timestamp 必须为正数";

    /** 缺少 secret。 */
    public static final String SECRET_MISSING = "请求缺少 secret";

    /** basePath 格式非法。 */
    public static final String BASE_PATH_INVALID = "basePath 必须以 / 开头且不以 / 结尾";

    /** query 编码非法。 */
    public static final String QUERY_ENCODING_INVALID = "query 参数编码非法";

    /** 请求体解密失败。 */
    public static final String BODY_DECRYPT_FAILED = "请求体解密失败";

    /** JDK 缺少 SHA-256。 */
    public static final String SHA256_UNAVAILABLE = "JDK 必须提供 SHA-256";

    /** JDK 缺少 HMAC-SHA256。 */
    public static final String HMAC_UNAVAILABLE = "JDK 必须提供 HMAC-SHA256";

    /** JDK 缺少 AES-256-GCM。 */
    public static final String AES_UNAVAILABLE = "JDK 必须提供 AES-256-GCM";

    /** 请求不是经典 HttpClient5 请求。 */
    public static final String HTTPCLIENT_NOT_CLASSIC = "仅支持经典 HttpClient5 请求";

    /** 请求缺少 URI。 */
    public static final String URI_MISSING = "请求缺少 URI";

    /** 请求 URI 非法。 */
    public static final String URI_INVALID = "请求 URI 非法";

    /** HttpClient5 entity 类型不受支持。 */
    public static final String HTTPCLIENT_ENTITY_UNSUPPORTED =
            "HttpClient5 只支持 StringEntity 或 ByteArrayEntity，流式、文件与 multipart entity 不受支持";

    /** RestTemplate multipart 不受支持。 */
    public static final String RESTTEMPLATE_MULTIPART_UNSUPPORTED =
            "RestTemplate 适配器不支持 multipart 请求";

    /** JDK HttpClient multipart 不受支持。 */
    public static final String JDK_MULTIPART_UNSUPPORTED =
            "JDK HttpClient 适配器不支持 multipart 请求";

    private SignMessages() {
    }

    /**
     * 缺少字段。
     *
     * @param field 字段名
     * @return 文案
     */
    public static String missing(String field) {
        return "请求缺少 " + field;
    }

    /**
     * header 名非法。
     *
     * @param name header 名
     * @return 文案
     */
    public static String invalidHeader(String name) {
        return "header 名不合法: " + name;
    }

    /**
     * header 名重复。
     *
     * @param name header 名
     * @return 文案
     */
    public static String duplicateHeader(String name) {
        return "header 名重复: " + name;
    }

    /**
     * 路径不包含 basePath。
     *
     * @param basePath 前缀
     * @return 文案
     */
    public static String basePathMismatch(String basePath) {
        return "请求路径不包含 basePath: " + basePath;
    }
}
