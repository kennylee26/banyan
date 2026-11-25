package cn.kennylee.banyan.spring.mvc.error;

/**
 * 错误响应契约：把异常上下文转换为响应体。使用方可提供自己的实现替换默认的中性结构。
 *
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface ExceptionResultFactory {

    /**
     * 构造错误响应体。
     *
     * @param context 错误上下文，非 {@code null}
     * @return 响应体对象，形状由实现决定
     */
    Object create(MvcErrorContext context);
}
