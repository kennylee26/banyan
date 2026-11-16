package cn.kennylee.banyan.data.core.query;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.util.Beans;
import cn.kennylee.banyan.commons.util.Strings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询策略抽象：把查询入参（Bean 或 Map）按「属性名 + 操作符后缀」解析成条件，再分派到具体实现的条件钩子。
 * <p>本类不引用任何具体数据库 API；{@code C} 是单条件容器，{@code Q} 是查询容器。</p>
 *
 * @param <E> 查询入参类型
 * @param <T> 实体类型
 * @param <C> 条件容器
 * @param <Q> 查询容器
 * @author kennylee
 * @since 0.1.0
 */
public abstract class AbstractDasQueryStrategy<E, T, C, Q> implements DasQueryStrategy<E, T> {

    /**
     * 解析查询入参为查询容器。
     *
     * @param searchExample 查询入参
     * @param entityClass 实体类型
     * @param ignoredProperties 需要忽略的属性
     * @return 结果对象
     */
    @Override
    public Object parseQuery(E searchExample, Class<T> entityClass, Collection<String> ignoredProperties) {
        Map<String, List<QueryCriteria>> criteriaByProperty = parseQueryCriteriaDtos(searchExample, entityClass, ignoredProperties);
        String firstProperty = criteriaByProperty.isEmpty() ? "" : criteriaByProperty.keySet().iterator().next();
        C criteria = newCriteria(firstProperty);
        parseQueryCriteriaDtos(criteria, criteriaByProperty);
        return getQuery();
    }

    /**
     * 把查询入参解析成「实体属性名 -> 条件列表」。
     *
     * @param entityClass 实体类型
     * @param ignoredProperties 需要忽略的属性
     * @param searchExample 查询入参
     * @return 返回结果
     */
    protected Map<String, List<QueryCriteria>> parseQueryCriteriaDtos(
            E searchExample, Class<T> entityClass, Collection<String> ignoredProperties) {
        Map<String, Object> props = getNotNullProperties(searchExample, ignoredProperties);
        Map<String, List<QueryCriteria>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String field = entry.getKey();
            CondOperation operation = CondOperation.match(field);
            String propName = operation.subFieldNameCamelCase(field);
            assertPropertyExists(entityClass, propName);
            result.computeIfAbsent(propName, key -> new ArrayList<>())
                    .add(new QueryCriteria(propName, operation, entry.getValue()));
        }
        return result;
    }

    /**
     * 条件容器的分派入口。
     *
     * @param criteria 条件容器
     * @param criteriaByProperty 按实体属性分组的查询条件
     */
    protected void parseQueryCriteriaDtos(C criteria, Map<String, List<QueryCriteria>> criteriaByProperty) {
        criteriaByProperty.forEach((propName, criterias) -> addCriteria(criteria, propName, criterias));
    }

    /**
     * 把同一个属性的条件列表分派到条件容器。
     *
     * @param criteria     条件容器
     * @param propName     实体属性名
     * @param criterias 该属性的条件列表
     */
    protected void addCriteria(C criteria, String propName, List<QueryCriteria> criterias) {
        String dbPropName = convertDbPropName(propName);
        for (QueryCriteria criterion : criterias) {
            dispatch(criteria, dbPropName, criterion);
        }
    }

    /**
     * 兼容入口：没有现成条件容器时按属性名新建一个再分派。
     *
     * @param criterias 该属性的条件列表
     * @param propName 属性名
     */
    protected void addCriteria(String propName, List<QueryCriteria> criterias) {
        addCriteria(newCriteria(propName), propName, criterias);
    }

    /** 按查询操作符分派到对应的条件钩子。 */
    private void dispatch(C criteria, String dbPropName, QueryCriteria criterion) {
        Object value = criterion.value();
        switch (criterion.condOperation()) {
            case EQ -> addEqCriteria(criteria, dbPropName, value);
            case NE -> addNeCriteria(criteria, dbPropName, value);
            case GT -> addGtCriteria(criteria, dbPropName, value);
            case GE -> addGeCriteria(criteria, dbPropName, value);
            case LT -> addLtCriteria(criteria, dbPropName, value);
            case LE -> addLeCriteria(criteria, dbPropName, value);
            case LIKE -> addLikeCriteria(criteria, dbPropName, value);
            case PRE_LIKE -> addPreLikeCriteria(criteria, dbPropName, value);
            case SUF_LIKE -> addSufLikeCriteria(criteria, dbPropName, value);
            case NOT_LIKE -> addNotLikeCriteria(criteria, dbPropName, value);
            case IN -> addInCriteria(criteria, dbPropName, asCollection(value));
            case NOT_IN -> addNotInCriteria(criteria, dbPropName, asCollection(value));
            case IS_NULL -> addIsNullCriteria(criteria, dbPropName);
            case IS_NOT_NULL -> addIsNotNullCriteria(criteria, dbPropName);
            default -> throw new BanyanException(ErrorCode.UNSUPPORTED_OPERATION, criterion.condOperation());
        }
    }

    /** 校验并转换 IN / NOT_IN 的集合值。 */
    private Collection<?> asCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        throw new BanyanException(ErrorCode.INVALID_COLLECTION_VALUE,
                value == null ? "null" : value.getClass().getName());
    }

    /**
     * 读取非空属性；Map 入参的键会转成驼峰，Bean 入参按 getter 读取。
     *
     * @param ignoredProperties 需要忽略的属性
     * @param searchExample 查询入参
     * @return 返回结果
     */
    protected Map<String, Object> getNotNullProperties(E searchExample, Collection<String> ignoredProperties) {
        if (searchExample == null) {
            return Collections.emptyMap();
        }
        Set<String> ignores = new HashSet<>();
        if (ignoredProperties != null) {
            ignores.addAll(ignoredProperties);
        }
        ignores.addAll(ignoreSearchProps());

        Map<String, Object> props = new LinkedHashMap<>();
        if (searchExample instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    props.put(Strings.toCamelCase(String.valueOf(entry.getKey())), entry.getValue());
                }
            }
        } else {
            props.putAll(Beans.toMap(searchExample, ignores));
        }
        props.keySet().removeAll(ignores);
        return props;
    }

    /** 校验属性在实体上存在。 */
    private void assertPropertyExists(Class<T> entityClass, String propName) {
        for (Class<?> current = entityClass; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(propName)) {
                    return;
                }
            }
        }
        throw new BanyanException(ErrorCode.UNKNOWN_PROPERTY, propName);
    }

    /**
     * 属性名转数据库字段名的扩展点，默认原样返回。
     *
     * @param propName 属性名
     * @return 返回结果
     */
    protected String convertDbPropName(String propName) {
        return propName;
    }

    /**
     * 需要从查询条件中排除的字段，默认空集。
     *
     * @return 返回结果
     */
    protected Collection<String> ignoreSearchProps() {
        return Collections.emptySet();
    }

    /**
     * 创建条件容器。
     *
     * @param propName 属性名
     * @return 返回结果
     */
    protected abstract C newCriteria(String propName);

    /**
     * 获取查询容器。
     *
     * @return 返回结果
     */
    protected abstract Q getQuery();

    /**
     * 添加相等条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addEqCriteria(C criteria, String propName, Object value);

    /**
     * 添加不等条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addNeCriteria(C criteria, String propName, Object value);

    /**
     * 添加大于条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addGtCriteria(C criteria, String propName, Object value);

    /**
     * 添加大于等于条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addGeCriteria(C criteria, String propName, Object value);

    /**
     * 添加小于条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addLtCriteria(C criteria, String propName, Object value);

    /**
     * 添加小于等于条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addLeCriteria(C criteria, String propName, Object value);

    /**
     * 添加全模糊条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addLikeCriteria(C criteria, String propName, Object value);

    /**
     * 添加前模糊条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addPreLikeCriteria(C criteria, String propName, Object value);

    /**
     * 添加后模糊条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addSufLikeCriteria(C criteria, String propName, Object value);

    /**
     * 添加不包含条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addNotLikeCriteria(C criteria, String propName, Object value);

    /**
     * 添加集合包含条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addInCriteria(C criteria, String propName, Collection<?> value);

    /**
     * 添加集合排除条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     * @param value 值
     */
    protected abstract void addNotInCriteria(C criteria, String propName, Collection<?> value);

    /**
     * 添加空值条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     */
    protected abstract void addIsNullCriteria(C criteria, String propName);

    /**
     * 添加非空条件。
     *
     * @param criteria 条件容器
     * @param propName 属性名
     */
    protected abstract void addIsNotNullCriteria(C criteria, String propName);
}
