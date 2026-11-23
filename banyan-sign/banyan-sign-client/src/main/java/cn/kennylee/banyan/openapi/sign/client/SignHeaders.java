package cn.kennylee.banyan.openapi.sign.client;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 五个签名参数的 HTTP header 名配置；调用方与提供方必须使用同一份配置。
 *
 * @param alg       算法标识的 header 名
 * @param keyId     密钥标识的 header 名
 * @param timestamp 毫秒时间戳的 header 名
 * @param nonce     一次性随机串的 header 名
 * @param sign      签名串的 header 名
 * @author kennylee
 * @since 0.1.0
 */
public record SignHeaders(String alg, String keyId, String timestamp,
                          String nonce, String sign) {

    /** RFC 7230 token 字符集。 */
    private static final Pattern TOKEN = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");

    /**
     * 默认 header 名。
     *
     * <p>{@link OpenApiRequestSigner.Builder} 默认已使用该值，通常不需要显式传入；
     * 只有需要引用默认名字时才使用。</p>
     */
    public static final SignHeaders DEFAULT = new SignHeaders(
            "X-Banyan-Alg", "X-Banyan-KeyId", "X-Banyan-Timestamp",
            "X-Banyan-Nonce", "X-Banyan-Sign");

    /**
     * 校验五个名字非空、字符合法且互不重复（HTTP header 名不区分大小写）。
     *
     * @throws IllegalArgumentException 存在空名、非法字符或重名
     */
    public SignHeaders {
        List<String> names = List.of(alg, keyId, timestamp, nonce, sign);
        Set<String> seen = new HashSet<>(names.size());
        for (String name : names) {
            if (name == null || name.isBlank() || !TOKEN.matcher(name).matches()) {
                throw new IllegalArgumentException(SignMessages.invalidHeader(name));
            }
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(SignMessages.duplicateHeader(name));
            }
        }
    }
}
