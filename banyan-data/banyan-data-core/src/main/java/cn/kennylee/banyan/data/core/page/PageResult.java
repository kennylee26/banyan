package cn.kennylee.banyan.data.core.page;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果。不可变值对象，用 record 表达。
 *
 * @param pageNo     页码
 * @param pageSize   每页条数
 * @param total      总记录数
 * @param totalPage  总页数
 * @param data       当前页数据
 * @param <T>        数据元素类型
 * @author kennylee
 * @since 0.1.0
 */
public record PageResult<T>(long pageNo, long pageSize, long total, long totalPage, List<T> data) {

    /**
     * 按「总数 + 数据」构造，自动计算总页数。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param total    总记录数
     * @param data     当前页数据
     */
    public PageResult(long pageNo, long pageSize, long total, List<T> data) {
        this(pageNo, pageSize, total, calcTotalPage(pageSize, total), data);
    }

    /**
     * 按数据条数推断总数的构造。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param data     当前页数据
     */
    public PageResult(long pageNo, long pageSize, List<T> data) {
        this(pageNo, pageSize, data == null ? 0 : data.size(), data);
    }

    private static long calcTotalPage(long pageSize, long total) {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * 创建空的分页结果。
     *
     * @param <T> 数据元素类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> emptyPage() {
        return new PageResult<>(PageDto.DEFAULT_PAGE_NO, PageDto.DEFAULT_PAGE_SIZE, 0, 0,
                Collections.emptyList());
    }
}
