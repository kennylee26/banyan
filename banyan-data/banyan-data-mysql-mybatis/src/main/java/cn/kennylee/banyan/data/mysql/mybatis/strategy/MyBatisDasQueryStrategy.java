package cn.kennylee.banyan.data.mysql.mybatis.strategy;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.query.AbstractDasQueryStrategy;
import cn.kennylee.banyan.data.core.query.DasQueryStrategy;
import cn.kennylee.banyan.data.mysql.mybatis.support.LikeEscaper;
import cn.kennylee.banyan.data.mysql.mybatis.wrapper.BanyanQueryWrapper;

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
    /** 是否对 like 值做特殊字符转义；全局层开启时由装配侧传 false。 */
    private final boolean likeEscapeEnabled;

    /**
     * 构造实例，默认开启转义。
     */
    public MyBatisDasQueryStrategy() {
        this(true);
    }

    /**
     * 构造实例，默认开启转义。
     *
     * @param entityClass 实体类型
     */
    public MyBatisDasQueryStrategy(Class<T> entityClass) {
        this(entityClass, true);
    }

    /**
     * 构造实例。
     *
     * @param likeEscapeEnabled 是否对 like 值做特殊字符转义
     */
    public MyBatisDasQueryStrategy(boolean likeEscapeEnabled) {
        this.likeEscapeEnabled = likeEscapeEnabled;
    }

    /**
     * 构造实例。
     *
     * @param entityClass       实体类型
     * @param likeEscapeEnabled 是否对 like 值做特殊字符转义
     */
    public MyBatisDasQueryStrategy(Class<T> entityClass, boolean likeEscapeEnabled) {
        this.entityClass = entityClass;
        this.likeEscapeEnabled = likeEscapeEnabled;
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
        if (!likeEscapeEnabled) {
            return String.valueOf(value);
        }
        return LikeEscaper.escape(String.valueOf(value));
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
        // 新实例必须继承父实例的转义开关，否则容器 bean 的配置会在复制后丢失
        return new MyBatisDasQueryStrategy<>(entityClass, likeEscapeEnabled);
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
