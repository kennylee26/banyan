package cn.kennylee.banyan.spring.mvc.handler;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.spring.mvc.error.DefaultExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.error.ErrorResponse;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import feign.FeignException;
import feign.codec.DecodeException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 异常 advice 行为测试：直接调用 + 真实 Spring MVC 装配。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExceptionAdviceTest {

    private final ExceptionResultFactory factory = new DefaultExceptionResultFactory();

    /** 固定语言，避免断言依赖运行环境。 */
    @BeforeEach
    void setUpLocale() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    /** 复位语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 兜底异常返回 HTTP 200 与中性内部错误，不回显原始异常信息。 */
    @Test
    void fallbackReturnsNeutralInternalError() {
        // given
        BanyanFallbackExceptionAdvice advice = new BanyanFallbackExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response = advice.handle(new RuntimeException("secret-detail"));

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_INTERNAL_ERROR.messageKey());
        assertThat(body.message()).isEqualTo(Messages.get(ErrorCode.SPRING_MVC_INTERNAL_ERROR));
        assertThat(body.message()).doesNotContain("secret-detail");
    }

    /** 缺少请求参数返回参数不合法错误。 */
    @Test
    void missingParameterReturnsBadRequest() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response =
                advice.handleBadRequest(new MissingServletRequestParameterException("id", "String"));

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /** 绑定异常取首个字段错误信息。 */
    @Test
    void bindExceptionCarriesFirstFieldError() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);
        Form form = new Form();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("name", "NotBlank", "name 不能为空");
        BindException exception = new BindException(bindingResult);

        // when
        ResponseEntity<Object> response = advice.handleBindException(exception);

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_VALIDATION_FAILED.messageKey());
        assertThat(body.message()).contains("name 不能为空");
    }

    /** 字段错误没有默认消息时回退为空串，输出不含 null。 */
    @Test
    void bindExceptionWithoutDefaultMessageFallsBackToEmpty() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);
        Form form = new Form();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("name", "NotNull");
        BindException exception = new BindException(bindingResult);

        // when
        ResponseEntity<Object> response = advice.handleBindException(exception);

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).doesNotContain("null");
    }

    /** 非法参数返回参数不合法错误。 */
    @Test
    void illegalArgumentReturnsBadRequest() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response = advice.handleBadRequest(new IllegalArgumentException("x"));

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /** 参数类型不匹配返回参数不合法错误。 */
    @Test
    void typeMismatchReturnsBadRequest() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response = advice.handleBadRequest(
                new MethodArgumentTypeMismatchException("x", Integer.class, "age", null, null));

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /** 请求体不可读返回参数不合法错误。 */
    @Test
    void unreadableBodyReturnsBadRequest() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response =
                advice.handleBadRequest(new HttpMessageNotReadableException("bad body"));

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /** 上传超限返回参数不合法错误。 */
    @Test
    void maxUploadSizeReturnsBadRequest() {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);

        // when
        ResponseEntity<Object> response =
                advice.handleBadRequest(new MaxUploadSizeExceededException(1024L));

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /** 参数校验异常（MethodArgumentNotValidException）取首个字段错误。 */
    @Test
    void methodArgumentNotValidReturnsValidationFailed() throws Exception {
        // given
        BanyanWebExceptionAdvice advice = new BanyanWebExceptionAdvice(factory);
        Form form = new Form();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("name", "NotBlank", "name 不能为空");
        MethodParameter parameter = new MethodParameter(Form.class.getDeclaredMethod("getName"), -1);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        // when
        ResponseEntity<Object> response = advice.handleBindException(exception);

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_VALIDATION_FAILED.messageKey());
        assertThat(body.message()).contains("name 不能为空");
    }

    /** 约束违规取首个违规消息。 */
    @SuppressWarnings("unchecked")
    @Test
    void constraintViolationCarriesFirstMessage() {
        // given
        BanyanConstraintViolationAdvice advice = new BanyanConstraintViolationAdvice(factory);
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("age 必须大于 0");

        // when
        ResponseEntity<Object> response =
                advice.handle(new ConstraintViolationException(Set.of(violation)));

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_VALIDATION_FAILED.messageKey());
        assertThat(body.message()).contains("age 必须大于 0");
    }

    /** Feign 异常返回远程调用失败。 */
    @Test
    void feignExceptionReturnsFeignError() {
        // given
        BanyanFeignExceptionAdvice advice = new BanyanFeignExceptionAdvice(factory);
        FeignException exception = mock(FeignException.class);

        // when
        ResponseEntity<Object> response = advice.handle(exception);

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_FEIGN_ERROR.messageKey());
    }

    /** 解码异常返回远程调用失败。 */
    @Test
    void decodeExceptionReturnsFeignError() {
        // given
        BanyanFeignExceptionAdvice advice = new BanyanFeignExceptionAdvice(factory);
        DecodeException exception = mock(DecodeException.class);

        // when
        ResponseEntity<Object> response = advice.handle(exception);

        // then
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(ErrorCode.SPRING_MVC_FEIGN_ERROR.messageKey());
    }

    /** 三个 advice 的优先级符合设计。 */
    @Test
    void adviceOrdersAreDeclared() {
        // given
        Order fallback = BanyanFallbackExceptionAdvice.class.getAnnotation(Order.class);
        Order web = BanyanWebExceptionAdvice.class.getAnnotation(Order.class);
        Order feign = BanyanFeignExceptionAdvice.class.getAnnotation(Order.class);

        // when
        int fallbackOrder = fallback == null ? Ordered.LOWEST_PRECEDENCE : fallback.value();

        // then
        assertThat(fallbackOrder).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        assertThat(web).isNotNull();
        assertThat(web.value()).isEqualTo(1);
        assertThat(feign).isNotNull();
        assertThat(feign.value()).isEqualTo(2);
    }

    /** 真实 Spring MVC 装配下 advice 生效。 */
    @Test
    void adviceIsAppliedBySpringMvc() throws Exception {
        // given
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new BanyanWebExceptionAdvice(factory))
                .build();

        // when
        MvcResult result = mockMvc.perform(get("/illegal")).andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString())
                .contains(ErrorCode.SPRING_MVC_BAD_REQUEST.messageKey());
    }

    /**
     * 表单占位对象。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    static class Form {

        private String name;
    }

    /**
     * 抛出非法参数的控制器。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @RestController
    static class ThrowingController {

        /**
         * 抛出非法参数。
         *
         * @return 不会返回
         */
        @GetMapping("/illegal")
        String illegal() {
            throw new IllegalArgumentException("x");
        }
    }
}
