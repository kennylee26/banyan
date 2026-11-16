package cn.kennylee.banyan.data.core.validation;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.lang.reflect.Method;

/**
 * 分页入口的最大页码校验。
 * <p>独立成类，避免加载 {@link MaxPageValidator}（它实现 {@code ConstraintValidator}，
 * 而 {@code jakarta.validation-api} 是 provided+optional，未引入校验实现的环境会 NoClassDefFoundError）。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class PageLimit {

    /** 默认总记录数上限。 */
    private static final long DEFAULT_TOTAL_RECORDS = 100000L;

    /** 默认页码字段名。 */
    private static final String DEFAULT_PAGE_FIELD = "pageNo";

    /** 默认每页条数字段名。 */
    private static final String DEFAULT_SIZE_FIELD = "pageSize";

    private PageLimit() {
    }

    /**
     * 校验分页入参的最大页码；超出上限时抛出统一异常。
     *
     * @param pageDto 分页入参
     * @throws BanyanException 页码超出允许上限
     */
    public static void validate(Object pageDto) {
        if (pageDto == null) {
            return;
        }
        DynamicMaxPage annotation = pageDto.getClass().getAnnotation(DynamicMaxPage.class);
        long totalRecords = annotation == null ? DEFAULT_TOTAL_RECORDS : annotation.totalRecords();
        String pageField = annotation == null ? DEFAULT_PAGE_FIELD : annotation.pageField();
        String sizeField = annotation == null ? DEFAULT_SIZE_FIELD : annotation.sizeField();
        Long pageNo = readLong(pageDto, pageField);
        Long size = readLong(pageDto, sizeField);
        if (pageNo != null && size != null && size > 0 && pageNo > totalRecords / size) {
            throw new BanyanException(ErrorCode.MAX_PAGE_EXCEEDED);
        }
    }

    private static Long readLong(Object bean, String property) {
        String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            Method method = bean.getClass().getMethod(getter);
            Object value = method.invoke(bean);
            return value == null ? null : ((Number) value).longValue();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
