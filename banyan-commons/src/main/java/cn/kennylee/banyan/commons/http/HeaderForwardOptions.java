package cn.kennylee.banyan.commons.http;

import java.util.ArrayList;
import java.util.List;

/**
 * header 传播的公共配置项，供各出站客户端能力的配置类继承。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class HeaderForwardOptions {

    /** 应用后自动清理上下文。 */
    private boolean clearAfterApply = true;

    /** 额外阻塞头。 */
    private List<String> blockedHeaders = new ArrayList<>();

    /**
     * 应用后是否自动清理上下文。
     *
     * @return true 表示自动清理
     */
    public boolean isClearAfterApply() {
        return clearAfterApply;
    }

    /**
     * 设置应用后是否自动清理上下文。
     *
     * @param clearAfterApply true 表示自动清理
     */
    public void setClearAfterApply(boolean clearAfterApply) {
        this.clearAfterApply = clearAfterApply;
    }

    /**
     * 额外阻塞头。
     *
     * @return 可变的阻塞头列表
     */
    public List<String> getBlockedHeaders() {
        return blockedHeaders;
    }

    /**
     * 设置额外阻塞头。
     *
     * @param blockedHeaders 阻塞头列表
     */
    public void setBlockedHeaders(List<String> blockedHeaders) {
        this.blockedHeaders = blockedHeaders;
    }
}
