package cn.kennylee.banyan.excel.exporter;

import java.util.List;

/**
 * 批次数据源，由使用方实现，按游标返回一批待导出的数据。
 *
 * <p>实现方只负责取数，不持有写出状态；返回空集合或不足一批表示没有更多数据。
 * 数据源应遵守批次大小，返回不超过 {@code batchSize} 行。</p>
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public interface BatchSource<T> {

    /**
     * 加载下一批数据。
     *
     * @param cursor    导出游标
     * @param batchSize 本批最多返回的行数
     * @return 一批数据；为空表示没有更多数据
     */
    List<T> loadNextBatch(ExportCursor cursor, int batchSize);

    /**
     * 取一批数据中最后一条记录的业务 ID，用于游标（keyset）分页。
     *
     * <p>默认返回 {@code null}，表示不使用游标分页。实现方若不需游标分页可忽略此方法。</p>
     *
     * @param rows 刚加载的一批数据
     * @return 最后一条记录的业务 ID；不使用游标分页时返回 {@code null}
     */
    default String lastIdOf(List<T> rows) {
        return null;
    }
}
