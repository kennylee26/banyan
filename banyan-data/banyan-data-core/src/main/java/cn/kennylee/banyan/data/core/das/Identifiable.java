package cn.kennylee.banyan.data.core.das;

import java.io.Serializable;

/**
 * 统一主键契约：所有 DAS 实体都必须能读写主键。
 *
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
public interface Identifiable<P extends Serializable> {

    /**
     * 获取主键。
     *
     * @return 主键
     */
    P getId();

    /**
     * 设置主键。
     *
     * @param id 主键
     */
    void setId(P id);
}
