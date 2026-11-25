package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.error.MvcErrorContext;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;

/**
 * 异常 advice 共用的错误响应构造。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class MvcErrorResponses {

    private MvcErrorResponses() {
    }

    /**
     * 构造 HTTP 200 错误响应。
     *
     * @param factory   错误响应契约
     * @param errorCode 错误码
     * @param message   面向调用方的消息
     * @param cause     原始异常
     * @return HTTP 200 响应
     */
    static ResponseEntity<Object> ok(ExceptionResultFactory factory, ErrorCode errorCode,
                                     String message, Throwable cause) {
        return ResponseEntity.ok(factory.create(new MvcErrorContext(
                errorCode.messageKey(), message, cause)));
    }

    /**
     * 记录 WARN（含堆栈）并构造 HTTP 200 错误响应，消息取错误码对应文案。
     *
     * @param logger    advice 日志器
     * @param factory   错误响应契约
     * @param errorCode 错误码
     * @param cause     原始异常
     * @return HTTP 200 响应
     */
    static ResponseEntity<Object> warn(Logger logger, ExceptionResultFactory factory,
                                       ErrorCode errorCode, Throwable cause) {
        logger.warn(Messages.get(ErrorCode.SPRING_MVC_EXCEPTION_HANDLED,
                cause.getClass().getSimpleName()), cause);
        return ok(factory, errorCode, Messages.get(errorCode), cause);
    }
}
