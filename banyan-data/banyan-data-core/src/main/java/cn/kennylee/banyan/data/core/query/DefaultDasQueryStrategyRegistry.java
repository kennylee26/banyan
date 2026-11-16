package cn.kennylee.banyan.data.core.query;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认实现：构造注入初始策略，运行期可继续注册。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class DefaultDasQueryStrategyRegistry implements DasQueryStrategyRegistry {

    /** 数据库类型到策略的映射。 */
    private final Map<DasDbType, DasQueryStrategy<?, ?>> strategies = new ConcurrentHashMap<>();

    /**
     * 构造实例。
     *
     * @param strategies 初始策略集合
     */
    public DefaultDasQueryStrategyRegistry(Map<DasDbType, ? extends DasQueryStrategy<?, ?>> strategies) {
        if (strategies != null) {
            this.strategies.putAll(strategies);
        }
    }

    /**
     * 注册查询策略。
     *
     * @param dbType   数据库类型
     * @param strategy 查询策略
     */
    @Override
    public void register(DasDbType dbType, DasQueryStrategy<?, ?> strategy) {
        strategies.put(dbType, strategy);
    }

    /**
     * 按数据库类型获取查询策略。
     *
     * @param dbType 数据库类型
     * @return 查询策略；未注册时返回 null
     */
    @Override
    public DasQueryStrategy<?, ?> getStrategy(DasDbType dbType) {
        return strategies.get(dbType);
    }

    /**
     * 判断是否已注册该数据库类型的策略。
     *
     * @param dbType 数据库类型
     * @return 是否已注册
     */
    @Override
    public boolean containsStrategy(DasDbType dbType) {
        return strategies.containsKey(dbType);
    }

    /**
     * 移除该数据库类型的策略。
     *
     * @param dbType 数据库类型
     */
    @Override
    public void remove(DasDbType dbType) {
        strategies.remove(dbType);
    }
}
