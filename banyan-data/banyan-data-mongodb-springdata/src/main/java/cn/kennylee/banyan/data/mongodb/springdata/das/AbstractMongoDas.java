package cn.kennylee.banyan.data.mongodb.springdata.das;

import cn.kennylee.banyan.data.core.das.AbstractDas;
import cn.kennylee.banyan.data.core.page.SortDirection;
import cn.kennylee.banyan.data.core.page.SortDto;
import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.util.CollUtils;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.data.mongodb.springdata.utils.MongoDbDasHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * MongoDB 数据访问：实现公共 DAS 抽象里与 Mongo 相关的计数、分页与排序差异点，
 * 并提供单集合 CRUD、批量删除、按主键集合查询与流式查询。
 *
 * @param <T> 实体类型
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public abstract class AbstractMongoDas<T extends MongoDbEo<P>, P extends Serializable>
        extends AbstractDas<T, P, Query> {

    /** Mongo 操作模板。 */
    private final MongoTemplate mongoTemplate;

    /** 分页查询是否允许使用磁盘（大结果集排序时避免内存限制）。 */
    @Getter
    @Setter
    private boolean findPageAllowDiskUse = true;

    /**
     * 构造实例。
     *
     * @param mongoTemplate Mongo 操作模板
     * @param registry      查询策略注册表
     */
    protected AbstractMongoDas(MongoTemplate mongoTemplate, DasQueryStrategyRegistry registry) {
        super(registry, DasDbType.MONGODB);
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 获取 Mongo 操作模板。
     *
     * @return MongoTemplate
     */
    protected MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    /**
     * 保存单条记录。
     *
     * @param entity 实体
     * @return 保存后的实体
     */
    @Override
    public T save(T entity) {
        return mongoTemplate.save(entity);
    }

    /**
     * 批量插入记录。
     *
     * @param entities 实体集合
     * @return 插入后的实体列表；集合为空时返回空列表
     */
    @Override
    public List<T> saveBatch(Collection<T> entities) {
        return saveBatch(entities, false, true);
    }

    /**
     * 批量插入并忽略单条失败。
     *
     * @param entities 实体集合
     * @return 成功插入的实体列表
     */
    public List<T> saveBatchIgnoreErrors(Collection<T> entities) {
        return saveBatch(entities, true, false);
    }

    /**
     * 批量插入。非事务环境下，出错回滚通过显式删除已插入记录实现。
     *
     * @param entities        实体集合
     * @param ignoreErrors    是否忽略单条失败
     * @param rollbackOnError 失败时是否删除已插入记录
     * @return 成功插入的实体列表
     */
    private List<T> saveBatch(Collection<T> entities, boolean ignoreErrors, boolean rollbackOnError) {
        if (CollUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<T> entitiesToSave = new ArrayList<>(entities);
        // 忽略错误：UNORDERED 模式尽力写入，单条失败不影响其余
        if (ignoreErrors) {
            try {
                mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, getEntityClass())
                        .insert(entitiesToSave)
                        .execute();
            } catch (RuntimeException e) {
                log.warn(Messages.get(ErrorCode.MONGO_BATCH_IGNORED_ERRORS, e.getMessage()));
            }
            return entitiesToSave;
        }
        // 默认：整批插入，失败时按需回滚已写入记录
        try {
            return List.copyOf(mongoTemplate.insertAll(entitiesToSave));
        } catch (RuntimeException e) {
            if (rollbackOnError) {
                rollbackInserted(entitiesToSave);
            }
            throw new BanyanException(ErrorCode.MULTI_VALUE_INSERT_FAILED, e);
        }
    }

    /**
     * 删除本批次中已写入的记录，用于批量失败后的回滚。
     *
     * @param entities 本批次实体
     */
    private void rollbackInserted(List<T> entities) {
        List<P> insertedIds = entities.stream().map(T::getId).filter(Objects::nonNull).toList();
        if (insertedIds.isEmpty()) {
            return;
        }
        mongoTemplate.remove(new Query(Criteria.where(MongoDbDasHelper.MONGO_ID).in(insertedIds)), getEntityClass());
    }

    /**
     * 按主键删除。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(P id) {
        return mongoTemplate.remove(idQuery(id), getEntityClass()).getDeletedCount() > 0;
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 实体；不存在时返回 null
     */
    @Override
    public T findById(P id) {
        return mongoTemplate.findById(id, getEntityClass());
    }

    /**
     * 查询全部记录。
     *
     * @return 实体列表
     */
    @Override
    public List<T> findAll() {
        return mongoTemplate.findAll(getEntityClass());
    }

    /**
     * 统计全部记录数。
     *
     * @return 记录数
     */
    @Override
    public long count() {
        return mongoTemplate.count(new Query(), getEntityClass());
    }

    /**
     * 按主键更新（整条覆盖，主键不存在时插入）。
     * <p>{@link MongoTemplate#save(Object)} 失败时抛异常，成功即视为更新成功。</p>
     *
     * @param entity 实体
     * @return 成功时返回 true
     */
    @Override
    public boolean updateById(T entity) {
        // save 返回保存后的实体且不会为 null，失败以异常形式抛出
        mongoTemplate.save(entity);
        return true;
    }

    /**
     * 按主键批量更新：用批量操作逐文档 {@code $set} 各记录值非 null 的字段，未提供的字段保持原值。
     * <p>返回值是数据库报告的实际修改文档数，值未变化的文档不计入，因此可能小于入参记录数。</p>
     *
     * @param entities 实体集合
     * @return 实际修改的文档数
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键、只有主键的记录
     */
    @Override
    public int updateBatchById(Collection<T> entities) {
        List<T> entitiesToUpdate = prepareBatchUpdate(entities);
        if (entitiesToUpdate.isEmpty()) {
            return 0;
        }
        List<Update> updates = new ArrayList<>(entitiesToUpdate.size());
        List<P> ids = new ArrayList<>(entitiesToUpdate.size());
        for (T entity : entitiesToUpdate) {
            updates.add(partialUpdate(entity, entity.getId()));
            ids.add(entity.getId());
        }
        // ORDERED：遇错即停并整体失败，与 MySQL 侧「整批不保留部分成功」的语义一致
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, getEntityClass());
        for (int i = 0; i < ids.size(); i++) {
            bulkOps.updateOne(idQuery(ids.get(i)), updates.get(i));
        }
        // BulkWriteResult#getModifiedCount() 本身返回 int，无需窄化
        return bulkOps.execute().getModifiedCount();
    }

    /**
     * 构造只包含非 null 字段的 {@code $set} 更新；未提供的字段不出现在更新内容里。
     *
     * @param entity 实体
     * @param id     主键，仅用于异常信息
     * @return 批量更新项
     * @throws IllegalArgumentException 记录没有任何非 null 的可更新字段
     */
    private Update partialUpdate(T entity, P id) {
        Document document = new Document();
        // 借 Spring Data 转换拿到 @Field 映射后的文档字段名，避免手写字段名映射
        mongoTemplate.getConverter().write(entity, document);
        // _id 与 _class 不是业务字段，不参与 $set；否则「只有主键」的判断会失效
        document.remove(MongoDbDasHelper.MONGO_ID);
        document.remove(MongoDbDasHelper.MONGO_CLASS);
        Update update = new Update();
        int setCount = 0;
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (entry.getValue() != null) {
                update.set(entry.getKey(), entry.getValue());
                setCount++;
            }
        }
        if (setCount == 0) {
            throw new IllegalArgumentException(Messages.get(
                    ErrorCode.BATCH_UPDATE_NO_UPDATABLE_FIELD, String.valueOf(id)));
        }
        return update;
    }

    /**
     * 按主键集合批量删除。
     *
     * @param ids 主键集合
     * @return 实际删除条数
     */
    public long removeByIds(Collection<P> ids) {
        if (CollUtils.isEmpty(ids)) {
            return 0;
        }
        return mongoTemplate.remove(new Query(Criteria.where(MongoDbDasHelper.MONGO_ID).in(ids)),
                getEntityClass()).getDeletedCount();
    }

    /**
     * 按主键集合查询。
     *
     * @param ids 主键集合
     * @return 实体列表；集合为空时返回空列表
     */
    public List<T> findByIds(Collection<P> ids) {
        if (CollUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return mongoTemplate.find(new Query(Criteria.where(MongoDbDasHelper.MONGO_ID).in(ids)),
                getEntityClass());
    }

    /**
     * 以流的方式读取全部记录。
     *
     * @return 实体流
     */
    public Stream<T> findAllStream() {
        return mongoTemplate.stream(new Query(), getEntityClass());
    }

    /**
     * 按查询容器统计记录数。
     *
     * @param query 查询容器
     * @return 记录数
     */
    @Override
    protected long countByQuery(Query query) {
        return mongoTemplate.count(query, getEntityClass());
    }

    /**
     * 按查询容器执行分页查询。
     *
     * @param query    查询容器
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 当前页数据
     */
    @Override
    protected List<T> queryPage(Query query, long pageNo, long pageSize) {
        query.allowDiskUse(findPageAllowDiskUse);
        query.with(PageRequest.of((int) (pageNo - 1), (int) pageSize));
        return mongoTemplate.find(query, getEntityClass());
    }

    /**
     * 把排序项应用到查询容器；无排序时追加 {@code _id} 升序以保证分页顺序可重复。
     *
     * @param query 查询容器
     * @param sorts 排序项
     */
    @Override
    protected void applySorts(Query query, List<SortDto> sorts) {
        if (sorts != null) {
            for (SortDto sort : sorts) {
                Sort.Direction direction = SortDirection.DESC == sort.getDirection()
                        ? Sort.Direction.DESC : Sort.Direction.ASC;
                query.with(Sort.by(direction, sort.getField()));
            }
        }
        if (query.getSortObject().isEmpty()) {
            query.with(Sort.by(Sort.Direction.ASC, MongoDbDasHelper.MONGO_ID));
        }
    }

    /** 按主键构造查询。 */
    private Query idQuery(P id) {
        return new Query(Criteria.where(MongoDbDasHelper.MONGO_ID).is(id));
    }
}
