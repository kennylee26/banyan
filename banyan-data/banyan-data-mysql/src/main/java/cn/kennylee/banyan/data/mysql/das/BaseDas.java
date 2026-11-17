package cn.kennylee.banyan.data.mysql.das;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
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
import cn.kennylee.banyan.commons.util.Strings;
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
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.ToIntFunction;

/**
 * MySQL 基础数据访问：实现公共 DAS 抽象里与 MyBatis 相关的计数、分页与排序差异点。
 *
 * @param <T> 实体类型
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public abstract class BaseDas<T extends Identifiable<P>, P extends Serializable>
        extends AbstractDas<T, P, BanyanQueryWrapper<T>> implements ApplicationContextAware {

    /** 默认批量提交条数。 */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /** 单条批量 UPDATE 默认允许更新的列数。 */
    private static final int DEFAULT_MAX_UPDATE_COLUMNS = 6;

    /** 单条批量 UPDATE 允许更新的列数硬上限。 */
    private static final int HARD_MAX_UPDATE_COLUMNS = 32;

    /** 单条批量 UPDATE 更新列数上限的配置项。 */
    private static final String MAX_UPDATE_COLUMNS_PROPERTY = "banyan.data-mysql.max-update-columns";

    /** ID 生成器，用于补齐主键。 */
    private IdProvider idProvider = new MybatisIdWorkerProvider();

    /** 主键生成策略，默认雪花。 */
    @Getter
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
        return saveBatch(entities, true);
    }

    /**
     * 批量保存记录，并指定是否启用单条多值 INSERT。
     * <p>{@code useBatchSql=false} 直接走框架自带批量操作，不做方言探测、不获取数据库连接；
     * {@code true} 时按方言判定选路，方言不支持则降级并输出 DEBUG 日志。</p>
     *
     * @param entities    实体集合
     * @param useBatchSql true 表示启用单条多值 INSERT，false 表示直接走框架自带批量
     * @return 保存后的实体列表
     */
    public List<T> saveBatch(Collection<T> entities, boolean useBatchSql) {
        if (CollUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<T> entitiesToSave = new ArrayList<>(entities);
        // 左侧为 false 时 && 短路，不触发方言探测、不获取连接
        boolean multiValueInsert = useBatchSql && supportsMultiValueInsertOnCurrentDataSource();
        if (useBatchSql && !multiValueInsert) {
            log.debug(Messages.get(ErrorCode.BATCH_SAVE_DIALECT_FALLBACK, entitiesToSave.size()));
        } else if (!useBatchSql) {
            log.debug(Messages.get(ErrorCode.BATCH_SAVE_SKIP_BY_CALLER, entitiesToSave.size()));
        }
        if (multiValueInsert) {
            // 手写 SQL 不经过 MP，按实体实际主键策略补 ID；写入主键后不再回填生成键
            for (T entity : entitiesToSave) {
                fillIdIfAbsent(entity);
            }
            log.debug(Messages.get(ErrorCode.BATCH_SAVE_MULTI_VALUE, entitiesToSave.size()));
            insertMultiValues(entitiesToSave, DEFAULT_BATCH_SIZE, true);
            return entitiesToSave;
        }
        // 框架批量：id 为空时由 @TableId 策略分配
        if (!Db.saveBatch(entitiesToSave, DEFAULT_BATCH_SIZE)) {
            throw new BanyanException(ErrorCode.BATCH_SAVE_FAILED);
        }
        return entitiesToSave;
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
        if (supportsMultiValueInsertOnCurrentDataSource()) {
            for (T entity : entitiesToSave) {
                fillIdIfAbsent(entity);
            }
            // 第三个参数为 true：把已补的主键写进 INSERT 列，避免被库自增覆盖
            insertMultiValues(entitiesToSave, batchSize, true);
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
            String column = sortColumn(sort.getField());
            if (SortDirection.DESC == sort.getDirection()) {
                query.orderByDesc(column);
            } else {
                query.orderByAsc(column);
            }
        }
    }

    /**
     * 把排序字段解析成数据库列名：实体属性名（驼峰）映射到 {@code @TableField} 声明的列名；
     * 已是列名或无法识别时原样返回，保留对原生 SQL 表达式的兼容。
     *
     * @param field 排序字段
     * @return 数据库列名或原值
     */
    private String sortColumn(String field) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        if (tableInfo == null) {
            return field;
        }
        if (field.equals(tableInfo.getKeyProperty())) {
            return tableInfo.getKeyColumn();
        }
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (field.equals(fieldInfo.getProperty())) {
                return fieldInfo.getColumn();
            }
        }
        return field;
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
        return insertMultiValues(entities, batchSize, false);
    }

    /**
     * 多值 INSERT 的统一实现。
     *
     * @param entities         实体集合
     * @param batchSize        单条 SQL 的最大行数
     * @param writeAssignedIds 是否把实体上已有的主键写进 INSERT 列；为 true 时不回填生成键
     * @return 实际插入条数
     */
    private int insertMultiValues(Collection<T> entities, int batchSize, boolean writeAssignedIds) {
        if (CollUtils.isEmpty(entities)) {
            return 0;
        }
        TableInfo tableInfo = requireTableInfo();
        List<T> entitiesToSave = prepareEntities(entities);
        List<TableFieldInfo> fields = tableInfo.getFieldList();
        return inSqlSession(session -> {
            int inserted = 0;
            // 按 batchSize 分片，避免单条 SQL 超过 max_allowed_packet
            for (List<T> chunk : CollUtils.partition(entitiesToSave, batchSize)) {
                inserted += insertChunk(session, tableInfo, fields, chunk, writeAssignedIds);
            }
            return inserted;
        }, ErrorCode.MULTI_VALUE_INSERT_FAILED);
    }

    /**
     * 在同一个 SqlSession 内执行分片操作。
     * <p>事务模式由 Spring 在方法结束后提交；非事务模式全部成功后统一 commit，
     * 任一分片失败时已执行分片整体回滚。非事务模式直接控制底层连接：
     * MyBatis-Spring 的连接只在 Spring 事务内才会被 {@code SqlSession.commit()} 真正提交。</p>
     *
     * @param work        使用当前 SqlSession 执行分片并返回影响行数
     * @param failureCode 连接或提交失败时使用的错误码
     * @return 各分片影响行数的累加值
     */
    private int inSqlSession(ToIntFunction<SqlSession> work, ErrorCode failureCode) {
        boolean transactional = TransactionSynchronizationManager.isSynchronizationActive();
        SqlSession session = transactional
                ? SqlSessionUtils.getSqlSession(sqlSessionFactory())
                : sqlSessionFactory().openSession();
        Connection connection = null;
        try {
            connection = session.getConnection();
            if (!transactional) {
                // MyBatis-Spring 的连接只在 Spring 事务内才提交，非事务模式需自行控制提交边界
                connection.setAutoCommit(false);
            }
            int affected = work.applyAsInt(session);
            if (!transactional) {
                connection.commit();
            }
            return affected;
        } catch (SQLException e) {
            rollbackQuietly(connection, transactional);
            throw new BanyanException(failureCode, e);
        } catch (RuntimeException e) {
            rollbackQuietly(connection, transactional);
            throw e;
        } finally {
            if (transactional) {
                SqlSessionUtils.closeSqlSession(session, sqlSessionFactory());
            } else {
                session.close();
            }
        }
    }

    /** 非事务模式执行失败后回滚；回滚本身失败只记录告警，不掩盖原始异常。 */
    private void rollbackQuietly(Connection connection, boolean transactional) {
        if (transactional || connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.warn(Messages.get(ErrorCode.BATCH_ROLLBACK_FAILED), e);
        }
    }

    /**
     * 插入一个分片并提交。
     *
     * @param session          当前 SqlSession
     * @param tableInfo        表信息
     * @param fields           需要写入的字段
     * @param chunk            当前分片的实体
     * @param writeAssignedIds 是否把实体上已有的主键写进 INSERT 列
     * @return 本分片插入条数
     */
    private int insertChunk(SqlSession session, TableInfo tableInfo, List<TableFieldInfo> fields,
                            List<T> chunk, boolean writeAssignedIds) {
        // 主键非空时写进 INSERT 列，与 MyBatis-Plus 的 ASSIGN_ID / INPUT 策略保持一致；
        // 主键全为空（AUTO）时省略主键列，交给数据库生成
        Field idField = findIdField(getEntityClass());
        boolean withId = writeAssignedIds && idField != null
                && chunk.stream().allMatch(entity -> entity.getId() != null);
        List<String> columns = fields.stream().map(TableFieldInfo::getColumn).toList();
        List<String> insertColumns = withId ? prependIdColumn(tableInfo.getKeyColumn(), columns) : columns;
        String sql = SqlBuilder.insertSql(tableInfo.getTableName(), insertColumns, chunk.size());
        // 要求驱动回传自增主键，供实体回填
        try (PreparedStatement statement = session.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(statement, fields, chunk, withId ? idField : null);
            int inserted = statement.executeUpdate();
            if (!withId) {
                // 多值 INSERT 的生成键按行顺序返回，逐一回填
                fillGeneratedKeys(statement, chunk);
            }
            return inserted;
        } catch (SQLException | IllegalAccessException e) {
            throw new BanyanException(ErrorCode.MULTI_VALUE_INSERT_FAILED, e);
        }
    }

    /** 把主键列放到列清单最前面，与参数绑定顺序一致。 */
    private List<String> prependIdColumn(String idColumn, List<String> columns) {
        List<String> insertColumns = new ArrayList<>(columns.size() + 1);
        insertColumns.add(idColumn);
        insertColumns.addAll(columns);
        return insertColumns;
    }

    /**
     * 按「实体 → 字段」顺序绑定占位符参数。
     *
     * @param statement 预编译语句
     * @param fields    字段列表
     * @param chunk     当前分片
     * @param idField   需要显式写入的主键字段；为 null 表示主键由数据库生成
     * @throws SQLException             参数绑定失败
     * @throws IllegalAccessException   字段不可访问
     */
    private void bindParameters(PreparedStatement statement, List<TableFieldInfo> fields, List<T> chunk,
                                Field idField) throws SQLException, IllegalAccessException {
        int index = 1;
        for (T entity : chunk) {
            if (idField != null) {
                idField.trySetAccessible();
                statement.setObject(index++, idField.get(entity));
            }
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
        return new ArrayList<>(entities);
    }

    /**
     * 按主键批量更新：只更新每条记录中值非 null 的字段，主键不进入更新内容。
     * <p>等价于 {@code updateBatchById(entities, true)}。返回值由数据库报告：单条批量 SQL 路径为
     * 各分片 {@code executeUpdate()} 的累加值，各数据库对「值未变化的行」计数口径不同，
     * 因此可能小于入参记录数，也可能等于匹配行数；走框架自带批量操作时返回 {@code -1}。</p>
     *
     * @param entities 实体集合
     * @return 数据库报告的更新行数；降级到框架批量操作时返回 -1
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键、只有主键的记录
     */
    @Override
    public int updateBatchById(Collection<T> entities) {
        return updateBatch(entities, DEFAULT_BATCH_SIZE, true);
    }

    /**
     * 按主键批量更新，并指定是否启用单条批量 UPDATE。
     * <p>{@code useBatchSql=false} 直接走框架自带批量操作，不做方言探测、不获取数据库连接；
     * {@code true} 时依次经过方言判定与字段并集上限两道闸门，任一不满足就整批降级并输出 DEBUG 日志。</p>
     *
     * @param entities    实体集合
     * @param useBatchSql true 表示启用单条批量 UPDATE，false 表示直接走框架自带批量
     * @return 数据库报告的更新行数；降级到框架批量操作时返回 -1
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键、只有主键的记录
     */
    public int updateBatchById(Collection<T> entities, boolean useBatchSql) {
        return updateBatch(entities, DEFAULT_BATCH_SIZE, useBatchSql);
    }

    /**
     * 按主键批量更新，并指定分片大小；batchSize 同时作用于单条批量 SQL 与降级路径。
     *
     * @param entities  实体集合
     * @param batchSize 每片最大行数
     * @return 数据库报告的更新行数；降级到框架批量操作时返回 -1
     * @throws IllegalArgumentException 入参为 null，或存在缺主键、重复主键、只有主键的记录
     */
    public int updateBatchById(Collection<T> entities, int batchSize) {
        return updateBatch(entities, batchSize, true);
    }

    /**
     * 批量更新的统一入口：两种选路共用前置校验。
     *
     * @param entities    实体集合
     * @param batchSize   每片最大行数
     * @param useBatchSql 是否启用单条批量 UPDATE
     * @return 数据库报告的更新行数；降级到框架批量操作时返回 -1
     */
    private int updateBatch(Collection<T> entities, int batchSize, boolean useBatchSql) {
        List<T> entitiesToUpdate = prepareBatchUpdate(entities);
        if (entitiesToUpdate.isEmpty()) {
            return 0;
        }
        TableInfo tableInfo = requireTableInfo();
        List<TableFieldInfo> updateFields = tableInfo.getFieldList();
        validateUpdatableFields(entitiesToUpdate, updateFields);
        // 显式关闭：跳过方言探测与连接获取，直接交给框架
        if (!useBatchSql) {
            log.debug(Messages.get(ErrorCode.BATCH_UPDATE_SKIP_BY_CALLER, entitiesToUpdate.size()));
            return updateByFramework(entitiesToUpdate, batchSize);
        }
        // 先解析上限：方言不支持时也要让非法配置告警
        int maxUpdateColumns = maxUpdateColumns();
        // 闸门按整批并集判断；分片内实际 SET 的列在 updateChunk 里按分片重算
        List<TableFieldInfo> setFields = nonNullUnionFields(entitiesToUpdate, updateFields);
        if (!supportsMultiValueInsertOnCurrentDataSource()) {
            log.debug(Messages.get(ErrorCode.BATCH_UPDATE_DIALECT_FALLBACK,
                    entitiesToUpdate.size(), setFields.size()));
            return updateByFramework(entitiesToUpdate, batchSize);
        }
        if (setFields.size() > maxUpdateColumns) {
            log.debug(Messages.get(ErrorCode.BATCH_UPDATE_COLUMNS_FALLBACK,
                    maxUpdateColumns, entitiesToUpdate.size(), setFields.size()));
            return updateByFramework(entitiesToUpdate, batchSize);
        }
        return updateByCaseWhen(tableInfo, setFields, entitiesToUpdate, batchSize);
    }

    /**
     * 校验每条记录至少有一个非 null 的可更新字段。
     *
     * @param entities     待更新实体
     * @param updateFields 可更新字段（不含主键）
     * @throws IllegalArgumentException 存在只有主键的记录
     */
    private void validateUpdatableFields(List<T> entities, List<TableFieldInfo> updateFields) {
        for (T entity : entities) {
            if (!hasNonNullField(entity, updateFields)) {
                throw new IllegalArgumentException(Messages.get(
                        ErrorCode.BATCH_UPDATE_NO_UPDATABLE_FIELD, String.valueOf(entity.getId())));
            }
        }
    }

    /**
     * 走框架自带批量操作，返回降级哨兵 {@code -1}。
     *
     * @param entities  待更新实体
     * @param batchSize 每片最大行数
     * @return 固定返回 -1
     */
    private int updateByFramework(List<T> entities, int batchSize) {
        if (!Db.updateBatchById(entities, batchSize)) {
            throw new BanyanException(ErrorCode.BATCH_UPDATE_FAILED);
        }
        return -1;
    }

    /**
     * 用单条 CASE WHEN UPDATE 分片执行批量更新。
     *
     * @param tableInfo    表信息
     * @param allSetFields 整批非 null 字段并集
     * @param entities     待更新实体
     * @param batchSize    每片最大行数
     * @return 各分片 {@code executeUpdate()} 的累加值
     */
    private int updateByCaseWhen(TableInfo tableInfo, List<TableFieldInfo> allSetFields,
                                 List<T> entities, int batchSize) {
        return inSqlSession(session -> {
            int affected = 0;
            // 分片执行，返回各片数据库原生计数的累加值
            for (List<T> chunk : CollUtils.partition(entities, batchSize)) {
                affected += updateChunk(session, tableInfo, allSetFields, chunk);
            }
            return affected;
        }, ErrorCode.BATCH_UPDATE_FAILED);
    }

    /**
     * 执行一个分片的 CASE WHEN 批量更新。SET 列取本分片的非 null 字段并集：
     * 整批并集中某个字段在本分片全为 null 时整列不出现，避免生成没有 WHEN 分支的 CASE。
     *
     * @param session      当前 SqlSession
     * @param tableInfo    表信息
     * @param allSetFields 整批非 null 字段并集
     * @param chunk        当前分片
     * @return 本分片数据库报告的更新行数
     */
    private int updateChunk(SqlSession session, TableInfo tableInfo,
                            List<TableFieldInfo> allSetFields, List<T> chunk) {
        List<TableFieldInfo> setFields = new ArrayList<>(allSetFields.size());
        List<String> setColumns = new ArrayList<>(allSetFields.size());
        List<Integer> valueCounts = new ArrayList<>(allSetFields.size());
        for (TableFieldInfo fieldInfo : allSetFields) {
            int count = 0;
            for (T entity : chunk) {
                if (readFieldValue(entity, fieldInfo) != null) {
                    count++;
                }
            }
            if (count == 0) {
                // 本分片该列全为 null：整列不出现，否则会生成没有 WHEN 分支的非法 CASE
                continue;
            }
            setFields.add(fieldInfo);
            setColumns.add(fieldInfo.getColumn());
            valueCounts.add(count);
        }
        String sql = SqlBuilder.updateCaseWhenSql(tableInfo.getTableName(), tableInfo.getKeyColumn(),
                setColumns, valueCounts, chunk.size());
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            bindCaseWhenParameters(statement, setFields, chunk);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new BanyanException(ErrorCode.BATCH_UPDATE_FAILED, e);
        }
    }

    /**
     * 按「列 → 行」顺序绑定 CASE WHEN 参数，最后绑定 WHERE 的主键参数。
     *
     * @param statement 预编译语句
     * @param setFields 本分片非 null 字段并集
     * @param chunk     当前分片
     * @throws SQLException 参数绑定失败
     */
    private void bindCaseWhenParameters(PreparedStatement statement, List<TableFieldInfo> setFields,
                                        List<T> chunk) throws SQLException {
        int index = 1;
        // 顺序必须与 updateCaseWhenSql 一致：逐列绑定「主键 + 值」，最后绑定 WHERE 的主键
        for (TableFieldInfo fieldInfo : setFields) {
            for (T entity : chunk) {
                Object value = readFieldValue(entity, fieldInfo);
                if (value == null) {
                    continue;
                }
                statement.setObject(index++, entity.getId());
                statement.setObject(index++, value);
            }
        }
        for (T entity : chunk) {
            statement.setObject(index++, entity.getId());
        }
    }

    /**
     * 取本批所有记录非 null 字段的并集，顺序与表字段顺序一致。
     *
     * @param entities     待更新实体
     * @param updateFields 可更新字段（不含主键）
     * @return 非 null 字段并集
     */
    private List<TableFieldInfo> nonNullUnionFields(List<T> entities, List<TableFieldInfo> updateFields) {
        List<TableFieldInfo> union = new ArrayList<>(updateFields.size());
        for (TableFieldInfo fieldInfo : updateFields) {
            for (T entity : entities) {
                if (readFieldValue(entity, fieldInfo) != null) {
                    union.add(fieldInfo);
                    break;
                }
            }
        }
        return union;
    }

    /**
     * 读取单条批量 UPDATE 允许的更新列数上限：默认 6，非法值或超过硬上限时回退默认值并告警。
     *
     * @return 生效的列数上限
     */
    private int maxUpdateColumns() {
        if (applicationContext == null) {
            return DEFAULT_MAX_UPDATE_COLUMNS;
        }
        return resolveMaxUpdateColumns(
                applicationContext.getEnvironment().getProperty(MAX_UPDATE_COLUMNS_PROPERTY));
    }

    /**
     * 解析列数上限配置值；空值取默认值，非法值（0、负数、非数字、超过硬上限）回退默认值并输出 WARN。
     *
     * @param rawValue 配置原值，可为 null
     * @return 生效的列数上限
     */
    static int resolveMaxUpdateColumns(String rawValue) {
        if (Strings.isBlank(rawValue)) {
            return DEFAULT_MAX_UPDATE_COLUMNS;
        }
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value > 0 && value <= HARD_MAX_UPDATE_COLUMNS) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // 非数字与越界值统一走下面的告警与回退
        }
        log.warn(Messages.get(ErrorCode.BATCH_UPDATE_MAX_COLUMNS_INVALID,
                MAX_UPDATE_COLUMNS_PROPERTY, rawValue, DEFAULT_MAX_UPDATE_COLUMNS, HARD_MAX_UPDATE_COLUMNS));
        return DEFAULT_MAX_UPDATE_COLUMNS;
    }

    /**
     * 判断实体是否至少有一个可更新字段非 null。
     *
     * @param entity       实体
     * @param updateFields 可更新字段
     * @return 是否存在非 null 字段
     */
    private boolean hasNonNullField(T entity, List<TableFieldInfo> updateFields) {
        for (TableFieldInfo fieldInfo : updateFields) {
            if (readFieldValue(entity, fieldInfo) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 反射读取实体字段值。
     *
     * @param entity    实体
     * @param fieldInfo 字段元数据
     * @return 字段值，可能为 null
     */
    private Object readFieldValue(T entity, TableFieldInfo fieldInfo) {
        try {
            Field field = fieldInfo.getField();
            field.trySetAccessible();
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new BanyanException(ErrorCode.BEAN_READ_FAILED, e);
        }
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
     * 判断某个方言是否支持多值 INSERT，是「扩展方言白名单」的覆写点：
     * 使用者可覆写本方法为第三方兼容库新增支持，也可把默认支持的方言判为不支持。
     * <p>与「按批次选路」的区别：本方法是方言能力判断，只决定当前数据源能否用多值 INSERT；
     * 显式选路请用 {@code saveBatch(Collection, boolean)}。</p>
     *
     * @param dbType  解析得到的数据库类型
     * @param jdbcUrl 原始 JDBC URL，供两个不同的 {@code DbType.OTHER} 库区分
     * @return 是否支持多值 INSERT
     */
    protected boolean supportsMultiValueInsert(DbType dbType, String jdbcUrl) {
        return DbDialect.supportsMultiValueInsert(dbType);
    }

    /**
     * 探测当前数据源是否支持多值 INSERT（MySQL / MariaDB / H2）。支持时用多值 VALUES 获得更高吞吐，
     * 其他方言（Oracle 等）回退到 JDBC 批量执行器。
     *
     * @return 是否支持多值 INSERT
     */
    private boolean supportsMultiValueInsertOnCurrentDataSource() {
        SqlSessionFactory sessionFactory = sqlSessionFactory();
        DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            return supportsMultiValueInsert(JdbcUtils.getDbType(jdbcUrl), jdbcUrl);
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
