package cn.kennylee.banyan.data.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按总记录数限制最大页码。默认总记录数上限 100000。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Documented
@Constraint(validatedBy = MaxPageValidator.class)
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicMaxPage {

    /**
     * 校验失败提示。
     *
     * @return 提示文案
     */
    String message() default "页码超出最大允许值";

    /**
     * 校验分组。
     *
     * @return 分组数组
     */
    Class<?>[] groups() default {};

    /**
     * 校验负载。
     *
     * @return 负载数组
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 页码字段名。
     *
     * @return 页码字段名
     */
    String pageField() default "pageNo";

    /**
     * 每页条数字段名。
     *
     * @return 每页条数字段名
     */
    String sizeField() default "pageSize";

    /**
     * 总记录数上限。
     *
     * @return 总记录数上限
     */
    long totalRecords() default 100000;
}
