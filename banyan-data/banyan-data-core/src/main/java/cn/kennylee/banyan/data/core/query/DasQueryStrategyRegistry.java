package cn.kennylee.banyan.data.core.query;

/**
 * 查询策略注册表。由容器管理，不使用进程级静态单例。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface DasQueryStrategyRegistry {

    /**
     * 注册查询策略。
     *
     * @param dbType   数据库类型
     * @param strategy 查询策略
     */
    void register(DasDbType dbType, DasQueryStrategy<?, ?> strategy);

    /**
     * 按数据库类型获取查询策略。
     *
     * @param dbType 数据库类型
     * @return 查询策略；未注册时返回 null
     */
    DasQueryStrategy<?, ?> getStrategy(DasDbType dbType);

    /**
     * 判断是否已注册该数据库类型的策略。
     *
     * @param dbType 数据库类型
     * @return 是否已注册
     */
    boolean containsStrategy(DasDbType dbType);

    /**
     * 移除该数据库类型的策略。
     *
     * @param dbType 数据库类型
     */
    void remove(DasDbType dbType);
}
