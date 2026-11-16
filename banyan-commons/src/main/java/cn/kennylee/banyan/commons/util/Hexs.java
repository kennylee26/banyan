package cn.kennylee.banyan.commons.util;

import java.util.HexFormat;

/**
 * 字节与十六进制文本互转（JDK 17 {@link HexFormat}）。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Hexs {

    private Hexs() {
    }

    /**
     * 编码。
     *
     * @param bytes 字节数组
     * @return 字符串
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * 解码。
     *
     * @param hex 十六进制文本
     * @return 返回结果
     */
    public static byte[] decode(String hex) {
        if (hex == null) {
            return null;
        }
        return HexFormat.of().parseHex(hex);
    }
}
