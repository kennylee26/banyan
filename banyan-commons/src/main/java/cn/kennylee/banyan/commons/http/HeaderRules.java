package cn.kennylee.banyan.commons.http;

import cn.kennylee.banyan.commons.util.CollUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * header 传播规则：默认阻塞名单与大小写无关的阻塞过滤。
 *
 * <p>默认阻塞连接与鉴权相关的头，避免自动把入站凭据带出去。
 * 纯 JDK 实现，不依赖任何 HTTP 客户端或 Servlet 容器。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class HeaderRules {

    /** 默认阻塞头，统一小写便于大小写无关匹配。 */
    private static final Set<String> DEFAULT_BLOCKED = Set.of(
            "cookie", "set-cookie", "content-length", "content-type", "accept", "authorization");

    private HeaderRules() {
    }

    /**
     * 剔除默认阻塞头与额外阻塞头，匹配忽略大小写。
     *
     * @param headers      待处理的 header 表，原地修改
     * @param extraBlocked 额外阻塞头，可为 null
     */
    public static void removeBlocked(Map<String, String> headers,
                                     Collection<String> extraBlocked) {
        removeBlocked(headers, extraBlocked, null);
    }

    /**
     * 剔除默认阻塞头、配置阻塞头与单次上下文阻塞头，匹配忽略大小写。
     *
     * @param headers 待处理的 header 表，原地修改
     * @param options 配置项，可为 null
     * @param context 单次上下文，可为 null
     */
    public static void removeBlocked(Map<String, String> headers,
                                     HeaderForwardOptions options,
                                     HeaderContext context) {
        removeBlocked(headers,
                options == null ? null : options.getBlockedHeaders(),
                context == null ? null : context.getBlockedHeaders());
    }

    /** 合并默认、配置与单次的阻塞头后统一剔除。 */
    private static void removeBlocked(Map<String, String> headers,
                                      Collection<String> extraBlocked,
                                      Collection<String> contextBlocked) {
        Set<String> blocked = new HashSet<>(DEFAULT_BLOCKED);
        blocked.addAll(CollUtils.lowerSet(extraBlocked));
        blocked.addAll(CollUtils.lowerSet(contextBlocked));
        headers.keySet().removeIf(name -> blocked.contains(name.toLowerCase(Locale.ROOT)));
    }
}
