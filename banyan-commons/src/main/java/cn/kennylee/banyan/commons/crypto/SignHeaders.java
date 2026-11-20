package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 签名参数的 HTTP header 名配置：调用方与提供方 MUST 共用同一份实例。
 *
 * <p>默认值见 {@link #DEFAULT}；需要自定义时直接构造，再分别传给
 * {@link RequestSigner} 与 {@link ReplayGuard}。</p>
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

    /** 字段名前缀，用于校验失败提示。 */
    private static final String FIELD = "header";

    /** RFC 7230 token：header 名只允许这些字符。 */
    private static final Pattern TOKEN = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");

    /** 默认 header 名。 */
    public static final SignHeaders DEFAULT = new SignHeaders(
            Signs.HEADER_ALG, Signs.HEADER_KEY_ID, Signs.HEADER_TIMESTAMP,
            Signs.HEADER_NONCE, Signs.HEADER_SIGN);

    /**
     * 校验五个名字：非空且互不相同（HTTP header 名不区分大小写）。
     */
    public SignHeaders {
        requireText(alg);
        requireText(keyId);
        requireText(timestamp);
        requireText(nonce);
        requireText(sign);
        Set<String> names = new HashSet<>(5);
        for (String name : List.of(alg, keyId, timestamp, nonce, sign)) {
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        Messages.get(ErrorCode.SIGN_REQUEST_INVALID, FIELD));
            }
        }
    }

    /** 必填校验，并要求符合 RFC 7230 token 字符集。 */
    private static void requireText(String value) {
        if (Strings.isBlank(value) || !TOKEN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, FIELD));
        }
    }
}
