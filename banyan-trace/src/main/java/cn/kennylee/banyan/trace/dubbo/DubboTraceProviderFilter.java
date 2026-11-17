package cn.kennylee.banyan.trace.dubbo;

import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceIdResolver;
import cn.kennylee.banyan.trace.support.TraceSettings;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Map;

/**
 * Dubbo 提供端过滤器：读取 attachment 写入线程上下文，上游无值时补生成，调用结束后恢复上下文。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Activate(group = CommonConstants.PROVIDER)
public class DubboTraceProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!TraceSettings.isDubboEnabled()) {
            return invoker.invoke(invocation);
        }
        String headerName = TraceSettings.getHeaderName();
        String incoming = invocation.getAttachment(headerName);
        String traceId = TraceIdResolver.resolveIncoming(
                incoming, TraceSettings.isAcceptIncoming());
        Map<String, String> mdcSnapshot = TraceContext.start(traceId, TraceSettings.getMdcKey());
        try {
            return invoker.invoke(invocation);
        } finally {
            TraceContext.stop(mdcSnapshot);
        }
    }
}
