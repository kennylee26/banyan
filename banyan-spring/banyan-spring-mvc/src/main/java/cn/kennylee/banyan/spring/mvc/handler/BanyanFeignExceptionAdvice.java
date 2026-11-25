package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import feign.FeignException;
import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Feign 异常处理：仅在 classpath 存在 Feign 时装配。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
@Order(BanyanFeignExceptionAdvice.ORDER)
@RequiredArgsConstructor
public class BanyanFeignExceptionAdvice {

    /** 处理顺序。 */
    public static final int ORDER = 2;

    private final ExceptionResultFactory resultFactory;

    /**
     * 处理 Feign 调用异常。
     *
     * @param exception Feign 异常或解码异常
     * @return HTTP 200 + 远程调用失败响应
     */
    @ExceptionHandler({FeignException.class, DecodeException.class})
    public ResponseEntity<Object> handle(Exception exception) {
        return MvcErrorResponses.warn(log, resultFactory, ErrorCode.SPRING_MVC_FEIGN_ERROR, exception);
    }
}
