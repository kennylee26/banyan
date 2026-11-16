package cn.kennylee.banyan.commons.util;

/**
 * 敏感信息脱敏。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Masks {

    private Masks() {
    }

    /**
     * 手机号脱敏：保留前 3 位与后 4 位；长度不足 7 位原样返回。
     *
     * @param mobile 手机号
     * @return 字符串
     */
    public static String maskMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        if (mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    /**
     * 邮箱脱敏：保留本地部分首字符与完整域名。
     *
     * @param email 邮箱
     * @return 字符串
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 1) {
            return local + "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
