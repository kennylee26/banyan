package cn.kennylee.banyan.spring.mvc.error;

/**
 * 默认错误响应实现：返回只含错误码与消息的中性结构。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class DefaultExceptionResultFactory implements ExceptionResultFactory {

    @Override
    public Object create(MvcErrorContext context) {
        return new ErrorResponse(context.code(), context.message());
    }
}
