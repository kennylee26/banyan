package cn.kennylee.banyan.commons.log;

import java.util.ArrayList;
import java.util.List;

/**
 * 出站请求日志配置：脱敏头、忽略头与 body 上限。
 *
 * <p>纯 JDK 配置结构，供各出站客户端能力的配置类直接持有。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class LoggingOptions {

    /** 默认 body 打印上限。 */
    public static final int DEFAULT_MAX_BODY_LENGTH = 2048;

    /** 敏感值脱敏。 */
    private boolean maskSensitive = true;

    /** 需要脱敏的 header。 */
    private List<String> maskedHeaders = new ArrayList<>(
            List.of("Authorization", "Cookie", "Set-Cookie"));

    /** 完全不打印的 header。 */
    private List<String> ignoredHeaders = new ArrayList<>();

    /** body 打印上限。 */
    private int maxBodyLength = DEFAULT_MAX_BODY_LENGTH;

    /**
     * 是否对敏感 header 脱敏。
     *
     * @return true 表示脱敏
     */
    public boolean isMaskSensitive() {
        return maskSensitive;
    }

    /**
     * 设置是否对敏感 header 脱敏。
     *
     * @param maskSensitive true 表示脱敏
     */
    public void setMaskSensitive(boolean maskSensitive) {
        this.maskSensitive = maskSensitive;
    }

    /**
     * 需要脱敏的 header。
     *
     * @return 可变的 header 列表
     */
    public List<String> getMaskedHeaders() {
        return maskedHeaders;
    }

    /**
     * 设置需要脱敏的 header。
     *
     * @param maskedHeaders header 列表
     */
    public void setMaskedHeaders(List<String> maskedHeaders) {
        this.maskedHeaders = maskedHeaders;
    }

    /**
     * 完全不打印的 header。
     *
     * @return 可变的 header 列表
     */
    public List<String> getIgnoredHeaders() {
        return ignoredHeaders;
    }

    /**
     * 设置完全不打印的 header。
     *
     * @param ignoredHeaders header 列表
     */
    public void setIgnoredHeaders(List<String> ignoredHeaders) {
        this.ignoredHeaders = ignoredHeaders;
    }

    /**
     * body 打印上限。
     *
     * @return 字符数上限
     */
    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    /**
     * 设置 body 打印上限。
     *
     * @param maxBodyLength 字符数上限
     */
    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }
}
