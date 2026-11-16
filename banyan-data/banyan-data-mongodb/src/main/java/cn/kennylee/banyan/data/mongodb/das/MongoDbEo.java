package cn.kennylee.banyan.data.mongodb.das;

import cn.kennylee.banyan.data.core.das.Identifiable;

import java.io.Serializable;

/**
 * MongoDB 实体契约。
 * <p>默认约定主键用 {@code org.bson.types.ObjectId}（插入时不设置、由 Mongo 自动生成）；
 * 需要自定义主键（如显式指定的 String / Long）时，直接以该类型作为泛型参数实现本接口即可。</p>
 *
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
public interface MongoDbEo<P extends Serializable> extends Identifiable<P> {
}
