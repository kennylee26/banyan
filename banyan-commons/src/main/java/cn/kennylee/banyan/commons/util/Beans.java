package cn.kennylee.banyan.commons.util;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Bean 工具。基于公共 getter 读取属性，只保留非 null 值。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Beans {

    private Beans() {
    }

    /**
     * 把 Bean 的非 null 属性转成 Map；{@code ignoredProperties} 中的属性会被跳过。
     *
     * @param bean Bean 对象
     * @param ignoredProperties 需要忽略的属性
     * @return 返回结果
     */
    public static Map<String, Object> toMap(Object bean, Collection<String> ignoredProperties) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (bean == null) {
            return result;
        }
        Set<String> ignores = ignoredProperties == null ? Collections.emptySet() : new HashSet<>(ignoredProperties);
        for (Method method : bean.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || void.class.equals(method.getReturnType())) {
                continue;
            }
            String property = propertyName(method);
            if (property == null || ignores.contains(property)) {
                continue;
            }
            try {
                method.trySetAccessible();
                Object value = method.invoke(bean);
                if (value != null) {
                    result.put(property, value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BanyanException(ErrorCode.BEAN_READ_FAILED, e, property);
            }
        }
        return result;
    }

    /** 从 getter 方法名解析属性名。 */
    private static String propertyName(Method method) {
        String name = method.getName();
        if ("getClass".equals(name)) {
            return null;
        }
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2
                && (boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType()))) {
            return decapitalize(name.substring(2));
        }
        return null;
    }

    /** 首字母转小写。 */
    private static String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
