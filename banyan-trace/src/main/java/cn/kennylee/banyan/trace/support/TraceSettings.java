package cn.kennylee.banyan.trace.support;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;

/**
 * 启动期解析后的 Trace 设置，供无法注入 Spring bean 的 SPI 实例与静态工具读取。
 * <p>只允许自动配置在启动期调用一次 {@link #init(TraceProperties)}，之后只读。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TraceSettings {

    /** 是否启用 Dubbo 透传。 */
    private static volatile boolean dubboEnabled = true;

    /** 传输 header / attachment 名。 */
    private static volatile String headerName = TraceProperties.DEFAULT_HEADER_NAME;

    /** MDC key。 */
    private static volatile String mdcKey = TraceProperties.DEFAULT_MDC_KEY;

    /** 是否沿用上游值。 */
    private static volatile boolean acceptIncoming = true;

    private TraceSettings() {
    }

    /**
     * 启动期写入配置。
     *
     * @param properties 配置
     */
    public static void init(TraceProperties properties) {
        dubboEnabled = properties.getDubbo().isEnabled();
        headerName = properties.getHeaderName();
        mdcKey = properties.getMdcKey();
        acceptIncoming = properties.isAcceptIncoming();
    }

    /**
     * 是否启用 Dubbo 透传。
     *
     * @return 是否启用
     */
    public static boolean isDubboEnabled() {
        return dubboEnabled;
    }

    /**
     * 获取 header / attachment 名。
     *
     * @return 名称
     */
    public static String getHeaderName() {
        return headerName;
    }

    /**
     * 获取 MDC key。
     *
     * @return MDC key
     */
    public static String getMdcKey() {
        return mdcKey;
    }

    /**
     * 是否沿用上游值。
     *
     * @return 是否沿用
     */
    public static boolean isAcceptIncoming() {
        return acceptIncoming;
    }
}
