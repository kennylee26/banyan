package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 方法参数校验异常处理：仅在 classpath 存在 jakarta.validation 时装配。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
@Order(BanyanConstraintViolationAdvice.ORDER)
@RequiredArgsConstructor
public class BanyanConstraintViolationAdvice {

    /** 处理顺序。 */
    public static final int ORDER = 1;

    private final ExceptionResultFactory resultFactory;

    /**
     * 处理约束违规异常。
     *
     * @param exception 约束违规异常
     * @return HTTP 200 + 校验失败响应（带首个违规消息）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handle(ConstraintViolationException exception) {
        String detail = firstMessage(exception);
        log.debug(Messages.get(ErrorCode.SPRING_MVC_EXCEPTION_HANDLED,
                exception.getClass().getSimpleName()));
        return MvcErrorResponses.ok(resultFactory, ErrorCode.SPRING_MVC_VALIDATION_FAILED,
                Messages.get(ErrorCode.SPRING_MVC_VALIDATION_FAILED, detail), exception);
    }

    private static String firstMessage(ConstraintViolationException exception) {
        Optional<ConstraintViolation<?>> first = exception.getConstraintViolations()
                .stream()
                .findFirst();
        return first.map(ConstraintViolation::getMessage).orElse("");
    }
}
