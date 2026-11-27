package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把一行的列值映射到目标类型的字段。
 *
 * <p>先由表头建立「列索引 → 字段」，再逐行用反射创建对象并设置字段；字段类型转换交给 {@link ValueConverters}。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
final class RowMapper<T> {

    private final Constructor<T> constructor;

    private final Map<Integer, Field> columnFields;

    private RowMapper(Constructor<T> constructor, Map<Integer, Field> columnFields) {
        this.constructor = constructor;
        this.columnFields = columnFields;
    }

    /**
     * 根据表头与别名建立映射。
     *
     * @param rowType       目标类型
     * @param headMap       表头：列索引 → 单元格
     * @param headerAliases 表头文案到字段名的别名
     * @param <T>           行类型
     * @return 行映射器
     */
    static <T> RowMapper<T> create(Class<T> rowType, Map<Integer, ReadCellData<?>> headMap,
                                   Map<String, String> headerAliases) {
        Constructor<T> constructor = noArgConstructor(rowType);
        Map<String, Field> fields = fieldsByName(rowType);
        Map<Integer, Field> columnFields = new HashMap<>();
        for (Map.Entry<Integer, ReadCellData<?>> entry : headMap.entrySet()) {
            ReadCellData<?> cell = entry.getValue();
            String text = cell == null ? null : cell.getStringValue();
            Field field = match(entry.getKey(), text, fields, headerAliases);
            if (field != null) {
                columnFields.put(entry.getKey(), field);
            }
        }
        return new RowMapper<>(constructor, Map.copyOf(columnFields));
    }

    /**
     * 把一行列值映射成目标对象。
     *
     * @param row 列索引 → 单元格字符串
     * @return 目标对象
     */
    T map(Map<Integer, String> row) {
        T instance;
        try {
            instance = constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new BanyanException(ErrorCode.PARSE_FAILED, e);
        }
        for (Map.Entry<Integer, String> entry : row.entrySet()) {
            Field field = columnFields.get(entry.getKey());
            if (field == null) {
                continue;
            }
            Object value = ValueConverters.convert(entry.getValue(), field.getType());
            if (value == null) {
                continue;
            }
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new BanyanException(ErrorCode.PARSE_FAILED, e);
            }
        }
        return instance;
    }

    private static Constructor<?> noArgConstructorFor(Class<?> rowType) {
        try {
            Constructor<?> constructor = rowType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new BanyanException(ErrorCode.PARSE_CONFIG_INVALID, "rowType");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> noArgConstructor(Class<T> rowType) {
        return (Constructor<T>) noArgConstructorFor(rowType);
    }

    private static Map<String, Field> fieldsByName(Class<?> rowType) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Field field : rowType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
        return fields;
    }

    private static Field match(int columnIndex, String text, Map<String, Field> fields,
                               Map<String, String> headerAliases) {
        if (text != null) {
            String aliasField = headerAliases.get(text);
            if (aliasField != null) {
                return fields.get(aliasField);
            }
            for (Field field : fields.values()) {
                ExcelProperty property = field.getAnnotation(ExcelProperty.class);
                if (property != null && Arrays.asList(property.value()).contains(text)) {
                    return field;
                }
                if (field.getName().equals(text)) {
                    return field;
                }
            }
        }
        for (Field field : fields.values()) {
            ExcelProperty property = field.getAnnotation(ExcelProperty.class);
            if (property != null && property.index() == columnIndex) {
                return field;
            }
        }
        return null;
    }
}
