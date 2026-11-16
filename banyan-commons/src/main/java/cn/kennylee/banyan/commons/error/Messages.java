package cn.kennylee.banyan.commons.error;

import java.util.Locale;

/**
 * 消息解析入口。默认按 JVM 的 {@link Locale} 解析，可显式切换语言或替换解析器。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Messages {

    /** 当前消息语言。 */
    private static volatile Locale locale = Locale.getDefault();

    /** 当前消息解析器。 */
    private static volatile MessageResolver resolver = new ResourceBundleMessageResolver(locale);

    private Messages() {
    }

    /**
     * 按错误码解析消息。
     *
     * @param errorCode 错误码
     * @param args      占位符参数
     * @return 解析后的消息
     */
    public static String get(ErrorCode errorCode, Object... args) {
        return resolver.resolve(errorCode.messageKey(), args);
    }

    /**
     * 切换消息语言。
     *
     * @param newLocale 目标语言
     */
    public static void setLocale(Locale newLocale) {
        locale = newLocale == null ? Locale.getDefault() : newLocale;
        resolver = new ResourceBundleMessageResolver(locale);
    }

    /**
     * 获取当前语言。
     *
     * @return 当前语言
     */
    public static Locale getLocale() {
        return locale;
    }

    /**
     * 替换消息解析器，例如 Spring 环境改用 MessageSource。
     *
     * @param newResolver 消息解析器
     */
    public static void setResolver(MessageResolver newResolver) {
        if (newResolver != null) {
            resolver = newResolver;
        }
    }
}
