package cn.kennylee.banyan.data.core.validation;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link DynamicMaxPage} 的校验实现：按 {@code totalRecords / pageSize} 计算最大页码。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MaxPageValidator implements ConstraintValidator<DynamicMaxPage, Object> {

    /** 页码字段名。 */
    private String pageField = "pageNo";
    /** 每页条数字段名。 */
    private String sizeField = "pageSize";
    /** 总记录数上限。 */
    private long totalRecords = 100000;

    /**
     * 初始化校验参数。
     *
     * @param annotation 注解实例
     */
    @Override
    public void initialize(DynamicMaxPage annotation) {
        this.pageField = annotation.pageField();
        this.sizeField = annotation.sizeField();
        this.totalRecords = annotation.totalRecords();
    }

    /**
     * 执行校验。
     *
     * @param value 值
     * @param context 校验上下文
     * @return 是否成功
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Long pageNo = readLong(value, pageField);
        Long pageSize = readLong(value, sizeField);
        if (pageNo == null || pageSize == null || pageSize <= 0) {
            return true;
        }
        long maxPage = totalRecords / pageSize;
        if (pageNo > maxPage) {
            // 用国际化消息替换默认模板；直接单测调用时 context 可能为 null
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        Messages.get(ErrorCode.MAX_PAGE_EXCEEDED)).addConstraintViolation();
            }
            return false;
        }
        return true;
    }



    /** 读取对象的数值字段。 */
    private Long readLong(Object bean, String property) {
        String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            Method method = bean.getClass().getMethod(getter);
            Object value = method.invoke(bean);
            return value == null ? null : ((Number) value).longValue();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // 没有 getter 时退回字段读取
        }
        try {
            Field field = findField(bean.getClass(), property);
            if (field == null) {
                return null;
            }
            field.trySetAccessible();
            Object value = field.get(bean);
            return value == null ? null : ((Number) value).longValue();
        } catch (IllegalAccessException e) {
            throw new BanyanException(ErrorCode.MAX_PAGE_READ_FAILED, e, property);
        }
    }

    /** 在类及其父类中查找字段。 */
    private Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }
        return null;
    }
}
