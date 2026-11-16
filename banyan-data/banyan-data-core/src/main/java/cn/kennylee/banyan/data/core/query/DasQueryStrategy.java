package cn.kennylee.banyan.data.core.query;

import java.util.Collection;

/**
 * 把查询入参解析成具体数据库查询容器的策略。公共层只定义契约，落地由各实现模块承担。
 *
 * @param <E> 查询入参类型
 * @param <T> 实体类型
 * @author kennylee
 * @since 0.1.0
 */
public interface DasQueryStrategy<E, T> {

    /**
     * 解析查询入参。
     *
     * @param searchExample     查询入参，支持普通 Bean 与 Map
     * @param entityClass       实体类型，用于校验字段是否存在
     * @param ignoredProperties 需要从查询条件中排除的属性
     * @return 具体数据库的查询容器
     */
    Object parseQuery(E searchExample, Class<T> entityClass, Collection<String> ignoredProperties);

    /**
     * 创建新的策略实例。策略实例持有查询过程中的临时状态，调用方应每次取新实例。
     *
     * @return 新的策略实例
     */
    DasQueryStrategy<E, T> newInstance();
}
