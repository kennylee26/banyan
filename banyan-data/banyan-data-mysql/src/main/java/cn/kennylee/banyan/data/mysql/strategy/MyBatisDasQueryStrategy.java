package cn.kennylee.banyan.data.mysql.strategy;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.AbstractDasQueryStrategy;
import cn.kennylee.banyan.data.core.query.DasQueryStrategy;
import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;

import java.util.Collection;

/**
 * MySQL / MyBatis 侧的查询策略：把公共条件落到 {@link BanyanQueryWrapper}，驼峰属性名转下划线列名。
 *
 * @param <T> 实体类型
 * @author kennylee
 * @since 0.1.0
 */
public class MyBatisDasQueryStrategy<T> extends
        AbstractDasQueryStrategy<Object, T, BanyanQueryWrapper<T>, BanyanQueryWrapper<T>> {

    /** 实体类型。 */
    private Class<T> entityClass;
    /** 当前查询容器。 */
    private BanyanQueryWrapper<T> wrapper;

    /**
     * 构造实例。
     */
    public MyBatisDasQueryStrategy() {
    }

    /**
     * 构造实例。
     *
     * @param entityClass 实体类型
     */
    public MyBatisDasQueryStrategy(Class<T> entityClass) {
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
    protected BanyanQueryWrapper<T> newCriteria(String propName) {
        wrapper = new BanyanQueryWrapper<>(entityClass);
        return wrapper;
    }

    @Override
    protected BanyanQueryWrapper<T> getQuery() {
        return wrapper;
    }

    @Override
    protected String convertDbPropName(String propName) {
        return Strings.toUnderlineCase(propName);
    }

    /**
     * 转义 LIKE 的特殊字符，避免用户输入中的 % 与 _ 被当作通配符。
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private String escapeLike(Object value) {
        return String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
        return new MyBatisDasQueryStrategy<>(entityClass);
    }

    @Override
    protected void addEqCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.eq(propName, value);
    }

    @Override
    protected void addNeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.ne(propName, value);
    }

    @Override
    protected void addGtCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.gt(propName, value);
    }

    @Override
    protected void addGeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.ge(propName, value);
    }

    @Override
    protected void addLtCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.lt(propName, value);
    }

    @Override
    protected void addLeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.le(propName, value);
    }

    @Override
    protected void addLikeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.like(propName, escapeLike(value));
    }

    @Override
    protected void addPreLikeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.likeRight(propName, escapeLike(value));
    }

    @Override
    protected void addSufLikeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.likeLeft(propName, escapeLike(value));
    }

    @Override
    protected void addNotLikeCriteria(BanyanQueryWrapper<T> criteria, String propName, Object value) {
        criteria.notLike(propName, escapeLike(value));
    }

    @Override
    protected void addInCriteria(BanyanQueryWrapper<T> criteria, String propName, Collection<?> value) {
        criteria.in(propName, value);
    }

    @Override
    protected void addNotInCriteria(BanyanQueryWrapper<T> criteria, String propName, Collection<?> value) {
        criteria.notIn(propName, value);
    }

    @Override
    protected void addIsNullCriteria(BanyanQueryWrapper<T> criteria, String propName) {
        criteria.isNull(propName);
    }

    @Override
    protected void addIsNotNullCriteria(BanyanQueryWrapper<T> criteria, String propName) {
        criteria.isNotNull(propName);
    }
}
