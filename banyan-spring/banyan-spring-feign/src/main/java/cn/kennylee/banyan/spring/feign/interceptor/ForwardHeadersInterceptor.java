package cn.kennylee.banyan.spring.feign.interceptor;

import cn.kennylee.banyan.commons.spring.web.HeaderContext;
import cn.kennylee.banyan.commons.spring.web.HeaderContextHolder;
import cn.kennylee.banyan.commons.spring.web.HeaderRules;
import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 入站 header 转发拦截器：默认由配置关闭，开启后按阻塞名单与单次上下文转发。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class ForwardHeadersInterceptor implements RequestInterceptor, Ordered {

    /** 配置。 */
    private final FeignProperties properties;

    /**
     * 构造拦截器。
     *
     * @param properties 配置
     */
    public ForwardHeadersInterceptor(FeignProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(@NonNull RequestTemplate template) {
        HeaderContext context = HeaderContextHolder.current();
        try {
            if (!HeaderContextHolder.isForwardEnabled()) {
                log.debug("当前 header 上下文或全局开关禁用转发");
                return;
            }
            Map<String, String> headers = HeaderRules.inboundHeaders();
            HeaderRules.removeBlocked(headers, blockedOf(context));
            headers.putAll(context.getCustomHeaders());
            headers.forEach(template::header);
        } finally {
            if (properties.getForwardHeaders().isClearAfterApply()
                    && context.isClearAtOnce()) {
                HeaderContextHolder.clear();
            }
        }
    }

    @Override
    public int getOrder() {
        return FeignRequestLogInterceptor.ORDER - 1;
    }

    /** 合并配置与单次的额外阻塞头。 */
    private List<String> blockedOf(HeaderContext context) {
        List<String> blocked = new ArrayList<>(
                properties.getForwardHeaders().getBlockedHeaders());
        blocked.addAll(context.getBlockedHeaders());
        return blocked;
    }
}
