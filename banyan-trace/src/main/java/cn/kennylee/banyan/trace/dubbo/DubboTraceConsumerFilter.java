package cn.kennylee.banyan.trace.dubbo;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.trace.support.TraceIdResolver;
import cn.kennylee.banyan.trace.support.TraceSettings;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo 消费端过滤器：把当前 TraceId 写入 attachment，已有则跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Activate(group = CommonConstants.CONSUMER)
public class DubboTraceConsumerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!TraceSettings.isDubboEnabled()) {
            return invoker.invoke(invocation);
        }
        String headerName = TraceSettings.getHeaderName();
        if (Strings.isBlank(invocation.getAttachment(headerName))) {
            String traceId = TraceIdResolver.resolve(TraceSettings.getMdcKey());
            if (Strings.isNotBlank(traceId)) {
                invocation.setAttachment(headerName, traceId);
            }
        }
        return invoker.invoke(invocation);
    }
}
