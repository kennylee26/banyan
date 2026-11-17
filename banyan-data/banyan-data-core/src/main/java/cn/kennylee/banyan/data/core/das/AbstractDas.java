package cn.kennylee.banyan.data.core.das;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.page.SortDto;
import cn.kennylee.banyan.data.core.validation.PageLimit;
import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategy;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * DAS 公共抽象：把「分页 DTO → 查询容器 → 计数 → 分页查询 → 组装结果」的流程写一次，
 * 各数据库只实现计数、分页查询与排序三个差异点。
 *
 * @param <T> 实体类型
 * @param <P> 主键类型
 * @param <Q> 具体数据库的查询容器类型
 * @author kennylee
 * @since 0.1.0
 */
public abstract class AbstractDas<T extends Identifiable<P>, P extends Serializable, Q>
        implements DasOperations<T, P> {

    /** 查询策略注册表。 */
    private final DasQueryStrategyRegistry registry;
    /** 本实现对应的数据库类型。 */
    private final DasDbType dbType;

    /**
     * 构造实例。
     *
     * @param registry 查询策略注册表
     * @param dbType   本实现对应的数据库类型
     */
    protected AbstractDas(DasQueryStrategyRegistry registry, DasDbType dbType) {
        this.registry = registry;
        this.dbType = dbType;
    }

    /**
     * 批量更新的整批前置校验：入参非 null、每条记录主键非空且不重复，返回入参副本。
     * <p>「每条记录至少有一个可更新字段」依赖各实现的元数据，由实现侧补充校验。</p>
     *
     * @param entities 实体集合
     * @return 入参副本；空集合时返回空列表（调用方据此 no-op）
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键的记录
     */
    protected List<T> prepareBatchUpdate(Collection<T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.BATCH_UPDATE_NULL_ARG));
        }
        List<T> entitiesToUpdate = new ArrayList<>(entities);
        Set<P> seenIds = new HashSet<>(entitiesToUpdate.size());
        for (T entity : entitiesToUpdate) {
            P id = entity.getId();
            if (id == null) {
                throw new IllegalArgumentException(Messages.get(ErrorCode.BATCH_UPDATE_ID_REQUIRED));
            }
            if (!seenIds.add(id)) {
                throw new IllegalArgumentException(Messages.get(
                        ErrorCode.BATCH_UPDATE_DUPLICATE_ID, String.valueOf(id)));
            }
        }
        return entitiesToUpdate;
    }

    /**
     * 按分页 DTO 执行分页查询。{@code searchCount} 三态：null 查总数与数据，true 仅查总数，false 仅查数据。
     *
     * @param pageDto 分页入参
     * @param <E>     分页入参类型
     * @return 分页结果
     */
    public <E extends PageDto> PageResult<T> findPage(E pageDto) {
        return findPage(pageDto, query -> {
        });
    }

    /**
     * 按分页 DTO 执行分页查询，并允许在查询前对查询容器做额外加工。
     *
     * @param pageDto    分页入参
     * @param customizer 查询容器加工器
     * @param <E>        分页入参类型
     * @return 分页结果
     */
    protected <E extends PageDto> PageResult<T> findPage(E pageDto, Consumer<Q> customizer) {
        // 分页入口统一触发最大页码校验，避免注解只在 Bean Validation 容器里生效
        PageLimit.validate(pageDto);
        long pageNo = pageDto.getPageNo() == null ? PageDto.DEFAULT_PAGE_NO : pageDto.getPageNo();
        long pageSize = pageDto.getPageSize() == null ? PageDto.DEFAULT_PAGE_SIZE : pageDto.getPageSize();

        Q query = createQuery(pageDto);
        customizer.accept(query);

        long total = 0;
        if (!Boolean.FALSE.equals(pageDto.getSearchCount())) {
            total = countByQuery(query);
        }
        List<T> data;
        if (Boolean.TRUE.equals(pageDto.getSearchCount())) {
            data = Collections.emptyList();
        } else {
            applySorts(query, pageDto.getSorts());
            data = queryPage(query, pageNo, pageSize);
        }
        return new PageResult<>(pageNo, pageSize, total, data);
    }

    /**
     * 把分页 DTO 解析成本数据库的查询容器，并应用排序。
     *
     * @param pageDto 分页入参
     * @return 查询容器
     */
    @SuppressWarnings("unchecked")
    protected Q createQuery(PageDto pageDto) {
        DasQueryStrategy<?, ?> registered = registry.getStrategy(dbType);
        if (registered == null) {
            throw new BanyanException(ErrorCode.STRATEGY_NOT_REGISTERED, dbType);
        }
        DasQueryStrategy<Object, T> strategy = (DasQueryStrategy<Object, T>) registered.newInstance();
        return (Q) strategy.parseQuery(pageDto, getEntityClass(), Collections.emptyList());
    }

    /**
     * 获取实体类型。
     *
     * @return 实体 Class
     */
    public abstract Class<T> getEntityClass();

    /**
     * 按查询容器统计记录数。
     *
     * @param query 查询容器
     * @return 记录数
     */
    protected abstract long countByQuery(Q query);

    /**
     * 按查询容器执行分页查询。
     *
     * @param query    查询容器
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 当前页数据
     */
    protected abstract List<T> queryPage(Q query, long pageNo, long pageSize);

    /**
     * 把排序项应用到查询容器；无排序时由实现决定是否追加稳定排序。
     *
     * @param query 查询容器
     * @param sorts 排序项
     */
    protected abstract void applySorts(Q query, List<SortDto> sorts);
}
