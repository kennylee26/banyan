package cn.kennylee.banyan.commons.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * UTF-8 文本的 Base64 编解码。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Base64s {

    private Base64s() {
    }

    /**
     * 编码。
     *
     * @param text 文本
     * @return 字符串
     */
    public static String encode(String text) {
        if (text == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码。
     *
     * @param encoded 参数
     * @return 字符串
     */
    public static String decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
