package cn.kennylee.banyan.commons.error;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 基于 JDK {@link ResourceBundle} 的消息解析器。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class ResourceBundleMessageResolver implements MessageResolver {

    /** 资源文件基名。 */
    private static final String BUNDLE_NAME = "banyan-messages";

    /**
     * 只查找「目标语言」与「基础 bundle」，不回退到 JVM 默认语言。
     * <p>默认行为会在找不到 zh_CN / zh 时回退到默认语言的 en bundle，
     * 导致中文环境读到英文消息。</p>
     */
    private static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return List.of(locale, Locale.ROOT);
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            // 禁用 JDK 默认的「回退到 JVM 默认语言」，否则 zh_CN 会读到 en bundle
            return null;
        }
    };

    /** 目标语言。 */
    private final Locale locale;

    /**
     * 构造实例。
     *
     * @param locale 目标语言
     */
    public ResourceBundleMessageResolver(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    @Override
    public String resolve(String key, Object... args) {
        String pattern;
        try {
            pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale, CONTROL).getString(key);
        } catch (MissingResourceException e) {
            // 资源缺失时返回 key，便于排查而未静默吞掉
            return key;
        }
        Object[] arguments = args == null ? new Object[0] : args;
        return arguments.length == 0 ? pattern : MessageFormat.format(pattern, arguments);
    }
}
