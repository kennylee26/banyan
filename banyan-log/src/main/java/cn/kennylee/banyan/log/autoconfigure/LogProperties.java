package cn.kennylee.banyan.log.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志配置。Java 侧只消费生命周期相关开关；logback 模板的变量由宿主的
 * {@code <springProperty>} 直接读取 {@code banyan.log.logback.*}，不在此重复声明默认值。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.log")
public class LogProperties {

    /** 装配总开关，默认 true；只管 Java 侧能力，不影响宿主 include 的 XML 模板。 */
    private boolean enabled = true;

    /** 生命周期日志配置。 */
    private Lifecycle lifecycle = new Lifecycle();

    /**
     * 生命周期日志配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Lifecycle {

        /** 生命周期日志开关，默认 true。 */
        private boolean enabled = true;

        /** 是否输出环境快照，默认 true。 */
        private boolean environmentEnabled = true;

        /**
         * 自定义启动横幅，支持 {@link java.text.MessageFormat} 占位符；为空时使用消息资源。
         *
         * <p>遵循 {@link java.text.MessageFormat} 语法：需要输出单引号时写两个单引号
         * （例如 {@code It''s {0}}），否则单引号会吞掉后续占位符。</p>
         */
        private String banner;
    }
}
