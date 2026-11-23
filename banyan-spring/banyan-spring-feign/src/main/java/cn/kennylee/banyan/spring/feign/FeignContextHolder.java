package cn.kennylee.banyan.spring.feign;

import java.util.Collection;
import java.util.Map;

/**
 * Feign 单次 header 上下文持有器。
 *
 * <p>默认每次 apply 后自动清理；支持单次禁用转发、追加阻塞头、追加自定义头，
 * 或关闭自动清理后由调用方自行 {@link #clear()}。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class FeignContextHolder {

    /** 线程级上下文。 */
    private static final ThreadLocal<FeignContext> HOLDER =
            ThreadLocal.withInitial(FeignContext::new);

    /** 全局开关；关闭时所有 header 转发停用。 */
    private static volatile boolean globallyEnabled = true;

    private FeignContextHolder() {
    }

    /**
     * 设置全局 header 转发开关。
     *
     * @param value true 表示允许转发
     */
    public static void setGloballyEnabled(boolean value) {
        globallyEnabled = value;
    }

    /**
     * 当前是否允许 header 转发。
     *
     * @return 全局开关与单次开关都为 true 时返回 true
     */
    public static boolean isForwardEnabled() {
        return globallyEnabled && current().isForwardHeaders();
    }

    /**
     * 获取当前上下文；不存在时创建默认上下文。
     *
     * @return 上下文
     */
    public static FeignContext current() {
        return HOLDER.get();
    }

    /**
     * 清理当前上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 单次启用 header 转发。
     */
    public static void enableForwardHeaders() {
        current().setForwardHeaders(true);
    }

    /**
     * 单次禁用 header 转发。
     */
    public static void disableForwardHeaders() {
        current().setForwardHeaders(false);
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
     * 追加自定义头，覆盖同名转发头。
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
     * 设置 apply 后是否自动清理。
     *
     * @param value true 表示自动清理
     */
    public static void setClearAtOnce(boolean value) {
        current().setClearAtOnce(value);
    }
}
