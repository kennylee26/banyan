package cn.kennylee.banyan.data.core.query;

/**
 * 单个查询条件：实体属性名（驼峰）、操作符与值。
 *
 * @param entityPropName 实体属性名（驼峰）
 * @param condOperation  查询操作符
 * @param value          条件值
 * @author kennylee
 * @since 0.1.0
 */
public record QueryCriteria(String entityPropName, CondOperation condOperation, Object value) {
}
