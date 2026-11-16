package cn.kennylee.banyan.data.core.das;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 数据访问统一契约：各数据库实现必须提供的基础能力。
 *
 * @param <T> 实体类型
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
public interface DasOperations<T extends Identifiable<P>, P extends Serializable> {

    /**
     * 保存单条记录。
     *
     * @param entity 实体
     * @return 保存后的实体
     */
    T save(T entity);

    /**
     * 批量保存记录。
     *
     * @param entities 实体集合
     * @return 保存后的实体列表
     */
    List<T> saveBatch(Collection<T> entities);

    /**
     * 按主键删除。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    boolean removeById(P id);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 实体；不存在时返回 null
     */
    T findById(P id);

    /**
     * 查询全部记录。
     *
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 统计记录数。
     *
     * @return 记录数
     */
    long count();

    /**
     * 按主键更新。
     *
     * @param entity 实体
     * @return 是否更新成功
     */
    boolean updateById(T entity);
}
