package cn.kennylee.banyan.commons.spring.web;

import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
public final class HeaderContext {

    /** 是否传播入站 header。 */
    private boolean forwardHeaders = true;

    /** 应用后是否自动清理。 */
    private boolean clearAtOnce = true;

    /** 阻塞头，忽略大小写，供外部追加。 */
    private final Set<String> blockedHeaders = new LinkedHashSet<>();

    /** 自定义头，覆盖同名传播头，供外部追加。 */
    private final Map<String, String> customHeaders = new LinkedHashMap<>();
}
