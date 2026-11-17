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

    /**
     * 批量按主键更新：只更新每条记录中值非 null 的字段，主键不进入更新内容。
     * <p>返回值由数据库报告：单条批量 SQL 路径为各分片 {@code executeUpdate()} 的累加值。
     * 各数据库对「值未变化的行」计数口径不同，因此该值可能小于入参记录数，也可能等于匹配行数；
     * 降级到框架批量操作的路径返回 {@code -1}，表示该路径不提供行数。</p>
     *
     * @param entities 实体集合
     * @return 数据库报告的更新行数；降级到框架批量操作时返回 -1
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键、只有主键的记录
     */
    int updateBatchById(Collection<T> entities);
}
