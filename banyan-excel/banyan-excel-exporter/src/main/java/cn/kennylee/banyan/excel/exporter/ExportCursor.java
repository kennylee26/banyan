package cn.kennylee.banyan.excel.exporter;

/**
 * 导出游标，描述下一批数据在整体导出中的位置。
 *
 * <p>页码分页使用 {@code batchIndex} 或 {@code exportedCount} 自行推算页码；
 * 游标（keyset）分页使用 {@code prevPageLastId}，该值由
 * {@link BatchSource#lastIdOf(java.util.List)} 在上一批写完后提供。</p>
 *
 * @param batchIndex     批次序号，从 0 开始
 * @param exportedCount  此前已导出的行数
 * @param prevPageLastId 上一批最后一条记录的业务 ID；首批或非游标分页时为 {@code null}
 * @author kennylee
 * @since 0.1.0
 */
public record ExportCursor(int batchIndex, int exportedCount, String prevPageLastId) {
}
