package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 兜底异常处理：未被其它 advice 处理的异常统一转换为内部错误响应。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class BanyanFallbackExceptionAdvice {

    private final ExceptionResultFactory resultFactory;

    /**
     * 处理未预期异常。
     *
     * @param exception 未预期异常
     * @return HTTP 200 + 内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handle(Exception exception) {
        return MvcErrorResponses.warn(log, resultFactory, ErrorCode.SPRING_MVC_INTERNAL_ERROR, exception);
    }
}
