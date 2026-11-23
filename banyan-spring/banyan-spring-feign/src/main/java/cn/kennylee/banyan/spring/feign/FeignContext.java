package cn.kennylee.banyan.spring.feign;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 单次 Feign 调用的 header 转发上下文。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
public final class FeignContext {

    /** 是否转发入站 header。 */
    private boolean forwardHeaders = true;

    /** apply 后是否自动清理。 */
    private boolean clearAtOnce = true;

    /** 阻塞头，忽略大小写，供外部追加。 */
    private final Set<String> blockedHeaders = new LinkedHashSet<>();

    /** 自定义头，覆盖同名转发头，供外部追加。 */
    private final Map<String, String> customHeaders = new LinkedHashMap<>();
}
