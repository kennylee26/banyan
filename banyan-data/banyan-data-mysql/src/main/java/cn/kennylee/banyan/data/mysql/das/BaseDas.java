package cn.kennylee.banyan.data.mysql.das;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.id.IdProvider;
import cn.kennylee.banyan.data.mysql.id.MybatisIdWorkerProvider;
import cn.kennylee.banyan.data.core.das.AbstractDas;
import cn.kennylee.banyan.data.core.das.Identifiable;
import cn.kennylee.banyan.data.core.page.PageResult;
import cn.kennylee.banyan.data.core.page.PageDto;
import cn.kennylee.banyan.data.core.page.SortDirection;
import cn.kennylee.banyan.data.core.page.SortDto;
import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.commons.util.CollUtils;
import cn.kennylee.banyan.data.mysql.support.DbDialect;
import cn.kennylee.banyan.data.mysql.support.SqlBuilder;
import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * MySQL 基础数据访问：实现公共 DAS 抽象里与 MyBatis 相关的计数、分页与排序差异点。
 *
 * @param <T> 实体类型
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
public abstract class BaseDas<T extends Identifiable<P>, P extends Serializable>
        extends AbstractDas<T, P, BanyanQueryWrapper<T>> implements ApplicationContextAware {

    /** 默认批量提交条数。 */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /** ID 生成器，用于补齐主键。 */
    private IdProvider idProvider = new MybatisIdWorkerProvider();

    /** 主键生成策略，默认雪花。 */
    private IdPolicy idPolicy = IdPolicy.ASSIGN_ID;

    /** 应用上下文。 */
    private ApplicationContext applicationContext;

    /**
     * 注入应用上下文，用于获取 Spring 管理的 SqlSessionFactory。
     *
     * @param applicationContext 应用上下文
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 从 Spring 容器获取 SqlSessionFactory，避免使用已过时的静态工具方法。
     *
     * @return SqlSessionFactory
     */
    private SqlSessionFactory sqlSessionFactory() {
        return applicationContext.getBean(SqlSessionFactory.class);
    }

    /**
     * 构造实例。
     *
     * @param registry 查询策略注册表
     */
    protected BaseDas(DasQueryStrategyRegistry registry) {
        super(registry, DasDbType.MYSQL);
    }

    /**
     * 设置主键生成策略，并同步到 MyBatis-Plus 的表元数据。
     * <p>同一实体类的 TableInfo 是共享的，设置后对该实体的所有插入路径生效。</p>
     *
     * @param idPolicy 主键生成策略
     */
    public void setIdPolicy(IdPolicy idPolicy) {
        if (idPolicy == null) {
            return;
        }
        this.idPolicy = idPolicy;
    }

    /**
     * 获取当前主键生成策略。
     *
     * @return 主键生成策略
     */
    public IdPolicy getIdPolicy() {
        return idPolicy;
    }


    private IdType toIdType(IdPolicy policy) {
        return switch (policy) {
            case AUTO -> IdType.AUTO;
            case ASSIGN_ID -> IdType.ASSIGN_ID;
            case ASSIGN_UUID -> IdType.ASSIGN_UUID;
            case INPUT -> IdType.INPUT;
        };
    }

    /**
     * 获取实体对应的底层 Mapper。
     *
     * @return 底层 Mapper
     */
    public abstract BaseMapper<T> getBaseMapper();

    /**
     * 替换 ID 生成器。用于规避 IdWorker 的重复 ID 问题，或接入自定义分布式 ID。
     *
     * @param idProvider ID 生成器
     */
    public void setIdProvider(IdProvider idProvider) {
        if (idProvider != null) {
            this.idProvider = idProvider;
        }
    }

    /**
     * 获取实体对应的 Mapper 类型，按需从 SqlSession 取 Mapper。
     *
     * @return Mapper 类型
     */
    public abstract Class<? extends BaseMapper<T>> getMapperClass();

    /**
     * 保存单条记录。
     *
     * @param entity 实体
     * @return 保存后的实体
     */
    @Override
    public T save(T entity) {
        getBaseMapper().insert(entity);
        return entity;
    }

    /**
     * 批量保存记录。
     *
     * @param entities 实体集合
     * @return 保存后的实体列表
     */
    @Override
    public List<T> saveBatch(Collection<T> entities) {
        return saveBatch(entities, DEFAULT_BATCH_SIZE);
    }

    /**
     * 使用 JDBC 批量执行器保存记录。整批在同一 SqlSession 内 flush/commit，失败时回滚。
     *
     * @param entities  实体集合
     * @param batchSize 每批提交条数
     * @return 保存后的实体列表
     */
    public List<T> saveBatch(Collection<T> entities, int batchSize) {
        if (CollUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        // 复制入参，避免修改调用方传入的集合
        List<T> entitiesToSave = new ArrayList<>(entities);
        // MySQL / MariaDB / H2 支持多值 INSERT：手写 JDBC 不经过 MP，需按 IdPolicy 补主键
        if (supportsMultiValueInsert()) {
            for (T entity : entitiesToSave) {
                fillIdIfAbsent(entity);
            }
            batchInsertForMySql(entitiesToSave, batchSize);
            return entitiesToSave;
        }
        // 其他方言交给 MyBatis-Plus：id 为空时由 @TableId 策略自动分配
        if (!Db.saveBatch(entitiesToSave, batchSize)) {
            throw new BanyanException(ErrorCode.BATCH_SAVE_FAILED);
        }
        return entitiesToSave;
    }


    /**
     * 手写 SQL 路径下，按实体实际的 {@code IdType} 决定是否补主键。
     * <p>与 MyBatis-Plus 的插入路径保持同一策略：AUTO / INPUT / NONE 不补；
     * ASSIGN_ID 补雪花；ASSIGN_UUID 补字符串。TableInfo 未初始化时回退到 {@link IdPolicy}。</p>
     *
     * @param entity 实体
     */
    private void fillIdIfAbsent(T entity) {
        if (entity.getId() != null) {
            return;
        }
        IdType effectiveType = effectiveIdType();
        if (effectiveType == IdType.AUTO || effectiveType == IdType.INPUT || effectiveType == IdType.NONE) {
            return;
        }
        Field idField = findIdField(entity.getClass());
        if (idField == null) {
            return;
        }
        Class<?> fieldType = idField.getType();
        try {
            idField.trySetAccessible();
            if (idField.get(entity) != null) {
                return;
            }
            if (effectiveType == IdType.ASSIGN_UUID || String.class.equals(fieldType)) {
                idField.set(entity, idProvider.nextId());
            } else if (Long.class.equals(fieldType) || long.class.equals(fieldType)) {
                idField.set(entity, idProvider.nextLongId());
            } else if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
                idField.set(entity, (int) (idProvider.nextLongId() & Integer.MAX_VALUE));
            }
        } catch (IllegalAccessException e) {
            throw new BanyanException(ErrorCode.ID_FILL_FAILED, e);
        }
    }

    /**
     * 读取实体在 MyBatis-Plus 中实际生效的主键策略。
     *
     * @return 主键策略；TableInfo 未初始化时按 IdPolicy 兜底
     */
    private IdType effectiveIdType() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        if (tableInfo != null && tableInfo.getIdType() != null) {
            return tableInfo.getIdType();
        }
        return switch (idPolicy) {
            case AUTO -> IdType.AUTO;
            case ASSIGN_ID -> IdType.ASSIGN_ID;
            case ASSIGN_UUID -> IdType.ASSIGN_UUID;
            case INPUT -> IdType.INPUT;
        };
    }

    /** 在类及其父类中查找主键字段。 */
    private Field findIdField(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if ("id".equals(field.getName())) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * 按主键删除。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(P id) {
        return getBaseMapper().deleteById(id) > 0;
    }

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 实体；不存在时返回 null
     */
    @Override
    public T findById(P id) {
        return getBaseMapper().selectById(id);
    }

    /**
     * 查询全部记录。
     *
     * @return 实体列表
     */
    @Override
    public List<T> findAll() {
        return getBaseMapper().selectList(null);
    }

    /**
     * 按条件查询全部记录。
     *
     * @param wrapper 查询条件
     * @return 实体列表
     */
    public List<T> findAll(Wrapper<T> wrapper) {
        return getBaseMapper().selectList(wrapper);
    }

    /**
     * 按条件查询单条记录。
     *
     * @param wrapper 查询条件
     * @return 实体；不存在时返回 null
     */
    public T findOne(Wrapper<T> wrapper) {
        return getBaseMapper().selectOne(wrapper);
    }

    /**
     * 统计全部记录数。
     *
     * @return 记录数
     */
    @Override
    public long count() {
        return getBaseMapper().selectCount(null);
    }

    /**
     * 按条件统计记录数。
     *
     * @param wrapper 查询条件
     * @return 记录数
     */
    public long count(Wrapper<T> wrapper) {
        return getBaseMapper().selectCount(wrapper);
    }

    /**
     * 按主键集合批量删除。
     *
     * @param ids 主键集合
     * @return 实际删除条数
     */
    public int removeByIds(Collection<? extends Serializable> ids) {
        if (CollUtils.isEmpty(ids)) {
            return 0;
        }
        return getBaseMapper().deleteBatchIds(ids);
    }

    /**
     * 按主键更新。
     *
     * @param entity 实体
     * @return 是否更新成功
     */
    @Override
    public boolean updateById(T entity) {
        return getBaseMapper().updateById(entity) > 0;
    }

    /**
     * 按查询容器统计记录数。
     *
     * @param query 查询容器
     * @return 记录数
     */
    @Override
    protected long countByQuery(BanyanQueryWrapper<T> query) {
        return getBaseMapper().selectCount(query);
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
    protected List<T> queryPage(BanyanQueryWrapper<T> query, long pageNo, long pageSize) {
        Page<T> page = new Page<>(pageNo, pageSize);
        return getBaseMapper().selectPage(page, query).getRecords();
    }

    /**
     * 把排序项应用到查询容器。
     *
     * @param query 查询容器
     * @param sorts 排序项
     */
    @Override
    protected void applySorts(BanyanQueryWrapper<T> query, List<SortDto> sorts) {
        if (CollUtils.isEmpty(sorts)) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
            query.orderByDesc(tableInfo == null ? "id" : tableInfo.getKeyColumn());
            return;
        }
        for (SortDto sort : sorts) {
            if (SortDirection.DESC == sort.getDirection()) {
                query.orderByDesc(sort.getField());
            } else {
                query.orderByAsc(sort.getField());
            }
        }
    }

    /**
     * 原生分页查询（返回 MyBatis-Plus 的 {@link IPage}）。
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @param <E>     分页类型
     * @return 分页结果
     */
    public <E extends IPage<T>> E selectPage(E page, Wrapper<T> wrapper) {
        return getBaseMapper().selectPage(page, wrapper);
    }

    /**
     * 无条件原生分页查询。
     *
     * @param page 分页对象
     * @param <E>  分页类型
     * @return 分页结果
     */
    public <E extends IPage<T>> E selectPage(E page) {
        return getBaseMapper().selectPage(page, null);
    }

    /**
     * 按分页 DTO 执行分页查询，并允许在查询前追加条件。
     *
     * @param pageDto      分页入参
     * @param consumerFunc 查询容器增强回调
     * @param <E>          分页入参类型
     * @return 分页结果
     */
    public <E extends PageDto> PageResult<T> findPage(E pageDto, ExtWrapperConsumerFunc<T> consumerFunc) {
        return findPage(pageDto, (Consumer<BanyanQueryWrapper<T>>) consumerFunc::accept);
    }

    /**
     * 使用单条多值 INSERT 批量保存。SQL 形如 {@code INSERT INTO t (c1,c2) VALUES (?,?),(?,?)}，
     * 分批提交；受数据库 {@code max_allowed_packet} 与 SQL 长度限制，大批量时请调小 batchSize。
     *
     * @param entities  实体集合
     * @param batchSize 单条 SQL 的最大行数
     * @return 实际插入条数
     */
    public int batchInsertForMySql(Collection<T> entities, int batchSize) {
        if (CollUtils.isEmpty(entities)) {
            return 0;
        }
        TableInfo tableInfo = requireTableInfo();
        List<T> entitiesToSave = prepareEntities(entities);
        List<TableFieldInfo> fields = tableInfo.getFieldList();
        List<String> columns = fields.stream().map(TableFieldInfo::getColumn).toList();

        boolean transactional = TransactionSynchronizationManager.isSynchronizationActive();
        SqlSession session = transactional
                ? SqlSessionUtils.getSqlSession(sqlSessionFactory())
                : sqlSessionFactory().openSession();
        try {
            int inserted = 0;
            // 按 batchSize 分片，避免单条 SQL 超过 max_allowed_packet
            for (List<T> chunk : CollUtils.partition(entitiesToSave, batchSize)) {
                inserted += insertChunk(session, tableInfo, fields, columns, chunk);
            }
            // 事务模式由 Spring 在方法结束后提交；非事务模式全部成功后再统一提交，
            // 保证任一分片失败时已执行的分片可以整体回滚
            if (!transactional) {
                session.commit();
            }
            return inserted;
        } catch (RuntimeException e) {
            if (!transactional) {
                session.rollback();
            }
            throw e;
        } finally {
            if (transactional) {
                SqlSessionUtils.closeSqlSession(session, sqlSessionFactory());
            } else {
                session.close();
            }
        }
    }

    /**
     * 插入一个分片并提交。
     *
     * @param session   当前 SqlSession
     * @param tableInfo 表信息
     * @param fields    需要写入的字段
     * @param columns   对应的列名
     * @param chunk     当前分片的实体
     * @return 本分片插入条数
     */
    private int insertChunk(SqlSession session, TableInfo tableInfo, List<TableFieldInfo> fields,
                            List<String> columns, List<T> chunk) {
        String sql = SqlBuilder.insertSql(tableInfo.getTableName(), columns, chunk.size());
        // 要求驱动回传自增主键，供实体回填
        try (PreparedStatement statement = session.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(statement, fields, chunk);
            int inserted = statement.executeUpdate();
            // 多值 INSERT 的生成键按行顺序返回，逐一回填
            fillGeneratedKeys(statement, chunk);
            return inserted;
        } catch (SQLException | IllegalAccessException e) {
            throw new BanyanException(ErrorCode.MULTI_VALUE_INSERT_FAILED, e);
        }
    }

    /**
     * 按「实体 → 字段」顺序绑定占位符参数。
     *
     * @param statement 预编译语句
     * @param fields    字段列表
     * @param chunk     当前分片
     * @throws SQLException             参数绑定失败
     * @throws IllegalAccessException   字段不可访问
     */
    private void bindParameters(PreparedStatement statement, List<TableFieldInfo> fields, List<T> chunk)
            throws SQLException, IllegalAccessException {
        int index = 1;
        for (T entity : chunk) {
            for (TableFieldInfo fieldInfo : fields) {
                Field field = fieldInfo.getField();
                field.trySetAccessible();
                Object value = field.get(entity);
                if (value == null) {
                    // 显式 setNull，保留数据库 NULL 语义
                    statement.setNull(index++, Types.NULL);
                } else {
                    statement.setObject(index++, value);
                }
            }
        }
    }

    /**
     * 获取实体表信息，缺失时快速失败。
     *
     * @return 表信息
     */
    private TableInfo requireTableInfo() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        // 表信息缺失通常意味着 Mapper 未注册，或实体未标注 @TableName
        if (tableInfo == null) {
            throw new BanyanException(ErrorCode.TABLE_INFO_NOT_FOUND, getEntityClass().getName());
        }
        return tableInfo;
    }

    /**
     * 复制实体集合并补齐主键（仅用于手写多值 INSERT，不经过 MyBatis-Plus）。
     *
     * @param entities 原实体集合
     * @return 可写入的实体列表
     */
    private List<T> prepareEntities(Collection<T> entities) {
        List<T> entitiesToSave = new ArrayList<>(entities);
        return entitiesToSave;
    }

    /**
     * 使用 JDBC 批量执行器按主键更新。
     *
     * @param entities 实体集合
     * @return 是否全部执行成功
     */
    public boolean updateBatchById(Collection<T> entities) {
        if (CollUtils.isEmpty(entities)) {
            return false;
        }
        return Db.updateBatchById(new ArrayList<>(entities));
    }

    /**
     * 使用 JDBC 批量执行器保存或更新：主键为空插入，否则按主键更新。
     *
     * @param entities 实体集合
     * @return 是否全部执行成功
     */
    public boolean saveOrUpdateBatch(Collection<T> entities) {
        if (CollUtils.isEmpty(entities)) {
            return false;
        }
        return Db.saveOrUpdateBatch(new ArrayList<>(entities));
    }

    /**
     * 使用 JDBC 批量执行器按主键删除。
     *
     * @param ids 主键集合
     * @return 实际执行的删除条数
     */
    public int removeBatchByIds(Collection<P> ids) {
        if (CollUtils.isEmpty(ids)) {
            return 0;
        }
        List<P> idsToRemove = new ArrayList<>(ids);
        return Db.removeByIds(idsToRemove, getEntityClass()) ? idsToRemove.size() : 0;
    }

    /**
     * 判断当前数据源是否支持多值 INSERT（MySQL / MariaDB / H2）。支持时用多值 VALUES 获得更高吞吐，
     * 其他方言（Oracle 等）回退到 JDBC 批量执行器。
     *
     * @return 是否支持多值 INSERT
     */
    private boolean supportsMultiValueInsert() {
        SqlSessionFactory sessionFactory = sqlSessionFactory();
        DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            DbType dbType = DbType.getDbType(connection.getMetaData().getURL());
            return DbDialect.supportsMultiValueInsert(dbType);
        } catch (SQLException e) {
            throw new BanyanException(ErrorCode.DIALECT_DETECT_FAILED, e);
        }
    }


    private void fillGeneratedKeys(PreparedStatement statement, List<T> entities) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            int index = 0;
            while (generatedKeys.next() && index < entities.size()) {
                Object key = generatedKeys.getObject(1);
                if (key != null) {
                    setEntityId(entities.get(index), key);
                }
                index++;
            }
        }
    }

    /** 按主键字段类型回填生成键。 */
    private void setEntityId(T entity, Object key) {
        Field idField = findIdField(entity.getClass());
        if (idField == null) {
            return;
        }
        try {
            idField.trySetAccessible();
            Class<?> idType = idField.getType();
            if (Long.class.equals(idType)) {
                idField.set(entity, ((Number) key).longValue());
            } else if (Integer.class.equals(idType)) {
                idField.set(entity, ((Number) key).intValue());
            } else if (String.class.equals(idType)) {
                idField.set(entity, String.valueOf(key));
            } else {
                idField.set(entity, key);
            }
        } catch (IllegalAccessException e) {
            throw new BanyanException(ErrorCode.GENERATED_KEY_FILL_FAILED, e);
        }
    }


    /**
     * 按实体类型构造空条件的自研查询容器，可直接传给 {@link #selectPage(IPage, Wrapper)}。
     *
     * @param eoClass 实体类型
     * @return 空条件的查询容器
     */
    public BanyanQueryWrapper<T> filter(Class<T> eoClass) {
        return new BanyanQueryWrapper<>(eoClass);
    }

    /**
     * 返回 MyBatis-Plus 本体的链式查询入口，适合 {@code .list()} / {@code .one()}。
     * <p>链式代理型容器不能作为 {@code Wrapper<T>} 传给 {@link #selectPage(IPage, Wrapper)}，
     * 分页请用 {@link #filter(Class)} 获取自研容器。</p>
     *
     * @return 链式查询容器
     */
    public QueryChainWrapper<T> filter() {
        return new QueryChainWrapper<>(getBaseMapper());
    }
}
