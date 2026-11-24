package cn.kennylee.banyan.commons.http;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 单次出站请求的 header 传播上下文。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class HeaderContext {

    /** 是否传播入站 header。 */
    private boolean propagateHeaders = true;

    /** 应用后是否自动清理。 */
    private boolean clearAtOnce = true;

    /** 阻塞头，忽略大小写，供外部追加。 */
    private final Set<String> blockedHeaders = new LinkedHashSet<>();

    /** 自定义头，覆盖同名传播头，供外部追加。 */
    private final Map<String, String> customHeaders = new LinkedHashMap<>();

    /**
     * 是否传播入站 header。
     *
     * @return true 表示传播
     */
    public boolean isPropagateHeaders() {
        return propagateHeaders;
    }

    /**
     * 设置是否传播入站 header。
     *
     * @param propagateHeaders true 表示传播
     */
    public void setPropagateHeaders(boolean propagateHeaders) {
        this.propagateHeaders = propagateHeaders;
    }

    /**
     * 应用后是否自动清理。
     *
     * @return true 表示自动清理
     */
    public boolean isClearAtOnce() {
        return clearAtOnce;
    }

    /**
     * 设置应用后是否自动清理。
     *
     * @param clearAtOnce true 表示自动清理
     */
    public void setClearAtOnce(boolean clearAtOnce) {
        this.clearAtOnce = clearAtOnce;
    }

    /**
     * 阻塞头集合，忽略大小写。
     *
     * @return 可变的阻塞头集合
     */
    public Set<String> getBlockedHeaders() {
        return blockedHeaders;
    }

    /**
     * 自定义头集合，覆盖同名传播头。
     *
     * @return 可变的自定义头集合
     */
    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }
}
