package cn.kennylee.banyan.spring.mvc.error;

/**
 * 默认错误响应体：只含错误码与消息，不夹带业务语义。
 *
 * @param code    错误码
 * @param message 错误消息
 * @author kennylee
 * @since 0.1.0
 */
public record ErrorResponse(String code, String message) {
}
