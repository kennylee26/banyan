package cn.kennylee.banyan.openapi.sign.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要验签与解密的 OpenAPI 接口：可标注在类或方法上，方法存在标注即生效。
 *
 * <p>具体密钥、header 名、时间窗口与加密开关全部在
 * {@code banyan.openapi-sign.*} 配置中维护，注解本身不接收参数。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApiSigned {
}
