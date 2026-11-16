package cn.kennylee.banyan.data.mysql.das;

import cn.kennylee.banyan.data.mysql.wrapper.BanyanQueryWrapper;

/**
 * 查询容器增强回调，供使用方在分页查询前追加条件。
 *
 * @param <T> 实体类型
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface ExtWrapperConsumerFunc<T> {

    /**
     * 处理查询容器。
     *
     * @param wrapper 查询容器
     */
    void accept(BanyanQueryWrapper<T> wrapper);
}
