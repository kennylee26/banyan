package cn.kennylee.banyan.openapi.sign.client;

import java.util.HexFormat;

/**
 * 日志与摘要共用的格式化工具：签名脱敏、字节转十六进制。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class Masks {

    /** 保留的签名字符数。 */
    private static final int KEEP = 8;

    private Masks() {
    }

    /**
     * 签名只保留前 8 位，避免完整签名进入日志。
     *
     * @param value 签名
     * @return 脱敏结果
     */
    static String first8(String value) {
        if (value == null) {
            return "<null>";
        }
        return value.length() <= KEEP ? value : value.substring(0, KEEP) + "...";
    }

    /**
     * 字节数组转小写十六进制。
     *
     * @param bytes 字节数组
     * @return 十六进制文本
     */
    static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
