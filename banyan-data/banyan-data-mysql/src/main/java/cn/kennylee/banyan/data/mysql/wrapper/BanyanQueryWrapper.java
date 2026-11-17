package cn.kennylee.banyan.data.mysql.wrapper;

import cn.kennylee.banyan.commons.util.Beans;
import cn.kennylee.banyan.commons.util.Strings;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * 自研查询容器：直接继承本体 {@link QueryWrapper}，因此可作为 {@code Wrapper<T>} 传给
 * {@code BaseMapper.selectPage}（链式代理型容器不能）。
 * <p>相比本体只补一件事：以方法引用指定列名。不实现连表 JOIN，不生成带别名的 SELECT 列。</p>
 * <p><b>LIKE 边界：</b>本类的 {@code like} / {@code notLike} 是原生透传，<b>不承诺</b>对
 * {@code %}、{@code _}、{@code \} 做特殊字符转义；需要转义时请使用查询策略 DTO
 * （受 {@code banyan.data-mysql.like-escape.enabled} 控制），或开启全局
 * {@code banyan.data-mysql.like-escape.inner-interceptor.enabled}。传入已转义的值不会被二次加工。</p>
 *
 * @param <T> 实体类型
 * @author kennylee
 * @since 0.1.0
 */
public class BanyanQueryWrapper<T> extends QueryWrapper<T> {

    /** 实体类型。 */
    private final Class<T> entityClass;

    /**
     * 构造实例。
     *
     * @param entityClass 实体类型
     */
    public BanyanQueryWrapper(Class<T> entityClass) {
        super();
        setEntityClass(entityClass);
        this.entityClass = entityClass;
    }

    /**
     * 按字段相等匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> eq(SFunction<T, ?> column, Object value) {
        super.eq(columnName(column), value);
        return this;
    }

    /**
     * 按字段不等匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> ne(SFunction<T, ?> column, Object value) {
        super.ne(columnName(column), value);
        return this;
    }

    /**
     * 按字段大于匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> gt(SFunction<T, ?> column, Object value) {
        super.gt(columnName(column), value);
        return this;
    }

    /**
     * 按字段大于等于匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> ge(SFunction<T, ?> column, Object value) {
        super.ge(columnName(column), value);
        return this;
    }

    /**
     * 按字段小于匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> lt(SFunction<T, ?> column, Object value) {
        super.lt(columnName(column), value);
        return this;
    }

    /**
     * 按字段小于等于匹配。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> le(SFunction<T, ?> column, Object value) {
        super.le(columnName(column), value);
        return this;
    }

    /**
     * 按字段全模糊匹配。原生透传，不承诺特殊字符转义；需要转义请用查询策略 DTO 或开启全局层。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> like(SFunction<T, ?> column, Object value) {
        super.like(columnName(column), value);
        return this;
    }

    /**
     * 按字段不包含匹配。原生透传，不承诺特殊字符转义；需要转义请用查询策略 DTO 或开启全局层。
     *
     * @param column 实体字段
     * @param value 值
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> notLike(SFunction<T, ?> column, Object value) {
        super.notLike(columnName(column), value);
        return this;
    }

    /**
     * 按字段集合包含匹配。
     *
     * @param column 实体字段
     * @param values 值集合
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> in(SFunction<T, ?> column, java.util.Collection<?> values) {
        super.in(columnName(column), values);
        return this;
    }

    /**
     * 按字段集合排除匹配。
     *
     * @param column 实体字段
     * @param values 值集合
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> notIn(SFunction<T, ?> column, java.util.Collection<?> values) {
        super.notIn(columnName(column), values);
        return this;
    }

    /**
     * 字段为空。
     *
     * @param column 实体字段
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> isNull(SFunction<T, ?> column) {
        super.isNull(columnName(column));
        return this;
    }

    /**
     * 字段非空。
     *
     * @param column 实体字段
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> isNotNull(SFunction<T, ?> column) {
        super.isNotNull(columnName(column));
        return this;
    }

    /**
     * 按字段升序。
     *
     * @param column 实体字段
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> orderByAsc(SFunction<T, ?> column) {
        super.orderByAsc(columnName(column));
        return this;
    }

    /**
     * 按字段降序。
     *
     * @param column 实体字段
     * @return 当前容器，便于链式调用
     */
    public BanyanQueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        super.orderByDesc(columnName(column));
        return this;
    }

    /** 把方法引用解析为数据库列名。 */
    private String columnName(SFunction<T, ?> column) {
        String methodName = LambdaUtils.extract(column).getImplMethodName();
        String property = Beans.propertyName(methodName);
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo != null) {
            if (property.equals(tableInfo.getKeyProperty())) {
                return tableInfo.getKeyColumn();
            }
            for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                if (fieldInfo.getProperty().equals(property)) {
                    return fieldInfo.getColumn();
                }
            }
        }
        return Strings.toUnderlineCase(property);
    }

}
