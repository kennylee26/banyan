package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.trace.context.TraceContext;
import io.micrometer.context.ContextRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * 响应式上下文传播配置：把 TraceId 线程上下文注册到 Reactor 自动上下文传播。
 * <p>开启后，Reactor 在操作符边界自动读写该线程上下文，响应式链跨线程时仍能取到 TraceId。
 * 该能力会启用 Reactor 的全局 Hook，属于库级副作用；可用
 * {@code banyan.trace.webflux.context-propagation.enabled=false} 关闭。</p>
 * <p>缺少 {@code io.micrometer:context-propagation} 时整个配置跳过，应用不受影响。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = {"io.micrometer.context.ContextRegistry", "reactor.core.publisher.Hooks"})
@ConditionalOnProperty(prefix = "banyan.trace", name = "enabled", matchIfMissing = true)
public class TraceContextPropagationAutoConfiguration {

    /**
     * WebFlux 关闭时整块跳过。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "banyan.trace.webflux", name = "enabled", matchIfMissing = true)
    static class WebfluxEnabled {

        /**
         * 响应式上下文传播开关打开时注册访问器并启用 Reactor 自动传播。
         *
         * @author kennylee
         * @since 0.1.0
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(prefix = "banyan.trace.webflux.context-propagation",
                name = "enabled", matchIfMissing = true)
        static class Enabled implements InitializingBean {

            @Override
            public void afterPropertiesSet() {
                ContextRegistry registry = ContextRegistry.getInstance();
                // 多次初始化时先移除同 key，保证幂等
                registry.removeThreadLocalAccessor(TraceContext.CONTEXT_KEY);
                registry.registerThreadLocalAccessor(new TraceIdThreadLocalAccessor());
                Hooks.enableAutomaticContextPropagation();
            }
        }
    }
}
