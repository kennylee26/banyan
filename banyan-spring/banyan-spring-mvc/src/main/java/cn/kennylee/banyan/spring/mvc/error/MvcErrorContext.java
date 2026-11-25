package cn.kennylee.banyan.spring.mvc.error;

/**
 * 错误响应上下文：错误码、可读消息与原始异常。
 *
 * @param code    稳定的错误码，来自 {@link cn.kennylee.banyan.commons.error.ErrorCode#messageKey()}
 * @param message 面向调用方的错误描述
 * @param cause   原始异常，可能为 {@code null}
 * @author kennylee
 * @since 0.1.0
 */
public record MvcErrorContext(String code, String message, Throwable cause) {
}
