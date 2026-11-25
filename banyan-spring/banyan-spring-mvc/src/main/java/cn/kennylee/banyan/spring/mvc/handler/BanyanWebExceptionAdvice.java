package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * MVC 参数与绑定异常处理：把客户端可纠正的异常转换为参数不合法响应。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
@Order(BanyanWebExceptionAdvice.ORDER)
@RequiredArgsConstructor
public class BanyanWebExceptionAdvice {

    /** 处理顺序。 */
    public static final int ORDER = 1;

    private final ExceptionResultFactory resultFactory;

    /**
     * 处理请求参数形态错误。
     *
     * @param exception 参数形态异常
     * @return HTTP 200 + 参数不合法响应
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MaxUploadSizeExceededException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Object> handleBadRequest(Exception exception) {
        log.debug(Messages.get(ErrorCode.SPRING_MVC_EXCEPTION_HANDLED,
                exception.getClass().getSimpleName()));
        return MvcErrorResponses.ok(resultFactory, ErrorCode.SPRING_MVC_BAD_REQUEST,
                Messages.get(ErrorCode.SPRING_MVC_BAD_REQUEST), exception);
    }

    /**
     * 处理参数绑定与校验失败。
     *
     * @param exception 绑定异常
     * @return HTTP 200 + 校验失败响应（带首个字段错误）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Object> handleBindException(BindException exception) {
        String detail = firstFieldMessage(exception.getBindingResult());
        log.debug(Messages.get(ErrorCode.SPRING_MVC_EXCEPTION_HANDLED,
                exception.getClass().getSimpleName()));
        return MvcErrorResponses.ok(resultFactory, ErrorCode.SPRING_MVC_VALIDATION_FAILED,
                Messages.get(ErrorCode.SPRING_MVC_VALIDATION_FAILED, detail), exception);
    }

    private static String firstFieldMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return "";
        }
        return fieldError.getDefaultMessage();
    }
}
