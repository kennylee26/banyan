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

    /** 对象标识 getter，不对应业务属性。 */
    private static final String CLASS_GETTER = "getClass";

    /** 普通 getter 前缀。 */
    private static final String GETTER_PREFIX = "get";

    /** 布尔 getter 前缀。 */
    private static final String BOOLEAN_GETTER_PREFIX = "is";

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
            if (!isReadableGetter(method)) {
                continue;
            }
            String property = propertyName(method);
            if (property == null || ignores.contains(property)) {
                continue;
            }
            Object value = readProperty(bean, method, property);
            if (value != null) {
                result.put(property, value);
            }
        }
        return result;
    }

    /** 只接收无参、有返回值的方法；getClass 与属性名解析交给调用方。 */
    private static boolean isReadableGetter(Method method) {
        return method.getParameterCount() == 0 && !void.class.equals(method.getReturnType());
    }

    /** 反射读取单个属性；非 null 值由调用方决定是否写入结果。 */
    private static Object readProperty(Object bean, Method method, String property) {
        try {
            method.trySetAccessible();
            return method.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BanyanException(ErrorCode.BEAN_READ_FAILED, e, property);
        }
    }

    /** 从 getter 方法解析属性名；布尔 getter 需要返回类型为 boolean / Boolean。 */
    private static String propertyName(Method method) {
        String name = method.getName();
        if (CLASS_GETTER.equals(name)) {
            return null;
        }
        String property = stripPrefix(name, GETTER_PREFIX);
        if (property != null) {
            return decapitalize(property);
        }
        boolean booleanGetter = boolean.class.equals(method.getReturnType())
                || Boolean.class.equals(method.getReturnType());
        if (!booleanGetter) {
            return null;
        }
        String booleanProperty = stripPrefix(name, BOOLEAN_GETTER_PREFIX);
        return booleanProperty == null ? null : decapitalize(booleanProperty);
    }

    /**
     * 从 getter 方法名解析属性名，供只拿得到方法名的调用方使用（如方法引用解析）。
     *
     * @param methodName 方法名
     * @return 属性名；不是 getter 时原样返回方法名
     */
    public static String propertyName(String methodName) {
        String property = stripPrefix(methodName, GETTER_PREFIX);
        if (property == null) {
            property = stripPrefix(methodName, BOOLEAN_GETTER_PREFIX);
        }
        return property == null ? methodName : decapitalize(property);
    }

    /**
     * 去掉方法名前缀；前缀后没有内容时返回 null。
     *
     * @param name   方法名
     * @param prefix 前缀
     * @return 去掉前缀后的名称；前缀不匹配或其后为空时返回 null
     */
    private static String stripPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return null;
        }
        return name.substring(prefix.length());
    }

    /**
     * 首字母转小写；前两个字母都大写时保持原样（与 JavaBeans 规范一致）。
     *
     * @param name 名称
     * @return 转换后的名称
     */
    public static String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
