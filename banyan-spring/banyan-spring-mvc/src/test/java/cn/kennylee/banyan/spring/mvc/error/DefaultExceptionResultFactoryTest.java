package cn.kennylee.banyan.spring.mvc.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认错误响应契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class DefaultExceptionResultFactoryTest {

    /** 默认实现返回 code 与 message 两个字段的中性结构。 */
    @Test
    void returnsNeutralErrorResponse() {
        // given
        ExceptionResultFactory factory = new DefaultExceptionResultFactory();
        MvcErrorContext context = new MvcErrorContext("code-1", "msg-1", new IllegalStateException("boom"));

        // when
        Object body = factory.create(context);

        // then
        assertThat(body).isInstanceOf(ErrorResponse.class);
        ErrorResponse response = (ErrorResponse) body;
        assertThat(response.code()).isEqualTo("code-1");
        assertThat(response.message()).isEqualTo("msg-1");
    }
}
