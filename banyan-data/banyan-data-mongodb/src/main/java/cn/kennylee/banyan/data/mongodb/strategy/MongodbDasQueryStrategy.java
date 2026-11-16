package cn.kennylee.banyan.data.mongodb.strategy;

import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.AbstractDasQueryStrategy;
import cn.kennylee.banyan.data.core.query.DasQueryStrategy;
import cn.kennylee.banyan.data.mongodb.utils.MongoDbDasHelper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MongoDB 查询策略：把公共条件落成 {@link Criteria}，多条件以 {@code $and} 合并。
 * <p>{@link Criteria} 是不可变对象，因此实际条件累积在本策略的列表里。</p>
 *
 * @param <T> 实体类型
 * @author kennylee
 * @since 0.1.0
 */
public class MongodbDasQueryStrategy<T> extends
        AbstractDasQueryStrategy<Object, T, Criteria, Query> {

    /** 实体类型。 */
    private Class<T> entityClass;
    /** 已累积的查询条件（Criteria 不可变，需单独保存）。 */
    private final List<Criteria> conditions = new ArrayList<>();

    /**
     * 构造实例。
     */
    public MongodbDasQueryStrategy() {
    }

    /**
     * 构造实例。
     *
     * @param entityClass 实体类型
     */
    public MongodbDasQueryStrategy(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 解析查询入参为查询容器。
     *
     * @param searchExample 查询入参
     * @param entityClass 实体类型
     * @param ignoredProperties 需要忽略的属性
     * @return 结果对象
     */
    @Override
    public Object parseQuery(Object searchExample, Class<T> entityClass,
                             Collection<String> ignoredProperties) {
        this.entityClass = entityClass;
        return super.parseQuery(searchExample, entityClass, ignoredProperties);
    }

    @Override
    protected Criteria newCriteria(String propName) {
        conditions.clear();
        return new Criteria();
    }

    @Override
    protected Query getQuery() {
        if (conditions.isEmpty()) {
            return new Query();
        }
        return new Query(new Criteria().andOperator(conditions));
    }

    @Override
    protected Collection<String> ignoreSearchProps() {
        return PageDto.PROPERTY_NAMES;
    }

    /**
     * 创建新的策略实例。
     *
     * @return 返回结果
     */
    @Override
    public DasQueryStrategy<Object, T> newInstance() {
        return new MongodbDasQueryStrategy<>(entityClass);
    }

    @Override
    protected void addEqCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).is(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addNeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).ne(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addGtCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).gt(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addGeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).gte(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addLtCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).lt(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addLeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).lte(MongoDbDasHelper.toEpochMilliIfNecessary(value)));
    }

    @Override
    protected void addLikeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).regex(MongoDbDasHelper.toLikeRegex(value)));
    }

    @Override
    protected void addPreLikeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).regex(MongoDbDasHelper.toPreLikeRegex(value)));
    }

    @Override
    protected void addSufLikeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).regex(MongoDbDasHelper.toSufLikeRegex(value)));
    }

    @Override
    protected void addNotLikeCriteria(Criteria criteria, String propName, Object value) {
        conditions.add(Criteria.where(propName).not().regex(MongoDbDasHelper.toLikeRegex(value)));
    }

    @Override
    protected void addInCriteria(Criteria criteria, String propName, Collection<?> value) {
        conditions.add(Criteria.where(propName).in(value));
    }

    @Override
    protected void addNotInCriteria(Criteria criteria, String propName, Collection<?> value) {
        conditions.add(Criteria.where(propName).nin(value));
    }

    @Override
    protected void addIsNullCriteria(Criteria criteria, String propName) {
        conditions.add(Criteria.where(propName).is(null));
    }

    @Override
    protected void addIsNotNullCriteria(Criteria criteria, String propName) {
        conditions.add(Criteria.where(propName).ne(null));
    }
}
