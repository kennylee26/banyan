package cn.kennylee.banyan.commons.http;

import java.util.Collection;
import java.util.Map;

/**
 * 出站请求 header 上下文的线程级持有器。
 *
 * <p>上下文按「一次出站调用」作用域使用：调用方在发起前设置，拦截器应用后自动清理。
 * 同一线程内串联多个出站能力时，可 {@link #setClearAtOnce(boolean)} 关闭自动清理，
 * 由调用方统一 {@link #clear()}，避免先执行的能力清掉后一个能力要用的 header。</p>
 *
 * <p>全局开关是进程级的总阀门，关闭后所有使用本上下文的出站能力都停止传播 header。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class HeaderContextHolder {

    /** 线程级上下文。 */
    private static final ThreadLocal<HeaderContext> HOLDER =
            ThreadLocal.withInitial(HeaderContext::new);

    /** 全局开关；关闭时所有 header 传播停用。 */
    private static volatile boolean globallyEnabled = true;

    private HeaderContextHolder() {
    }

    /**
     * 设置全局 header 传播开关。
     *
     * @param value true 表示允许传播
     */
    public static void setGloballyEnabled(boolean value) {
        globallyEnabled = value;
    }

    /**
     * 当前是否允许 header 传播。
     *
     * @return 全局开关与单次开关都为 true 时返回 true
     */
    public static boolean isPropagationEnabled() {
        return globallyEnabled && current().isPropagateHeaders();
    }

    /**
     * 获取当前上下文；不存在时创建默认上下文。
     *
     * @return 上下文
     */
    public static HeaderContext current() {
        return HOLDER.get();
    }

    /**
     * 清理当前上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 单次启用 header 传播。
     */
    public static void enablePropagation() {
        current().setPropagateHeaders(true);
    }

    /**
     * 单次禁用 header 传播。
     */
    public static void disablePropagation() {
        current().setPropagateHeaders(false);
    }

    /**
     * 追加阻塞头。
     *
     * @param headerName header 名
     */
    public static void addBlockedHeader(String headerName) {
        current().getBlockedHeaders().add(headerName);
    }

    /**
     * 追加阻塞头列表。
     *
     * @param headerNames header 名列表
     */
    public static void addBlockedHeaders(Collection<String> headerNames) {
        current().getBlockedHeaders().addAll(headerNames);
    }

    /**
     * 追加自定义头，覆盖同名传播头。
     *
     * @param name  header 名
     * @param value header 值
     */
    public static void addCustomHeader(String name, String value) {
        current().getCustomHeaders().put(name, value);
    }

    /**
     * 追加自定义头。
     *
     * @param headers header 表
     */
    public static void addCustomHeaders(Map<String, String> headers) {
        current().getCustomHeaders().putAll(headers);
    }

    /**
     * 替换自定义头。
     *
     * @param headers header 表
     */
    public static void setCustomHeaders(Map<String, String> headers) {
        current().getCustomHeaders().clear();
        current().getCustomHeaders().putAll(headers);
    }

    /**
     * 设置应用后是否自动清理。
     *
     * @param value true 表示自动清理
     */
    public static void setClearAtOnce(boolean value) {
        current().setClearAtOnce(value);
    }
}
