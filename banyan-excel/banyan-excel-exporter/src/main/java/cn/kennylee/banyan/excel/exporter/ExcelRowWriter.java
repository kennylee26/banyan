package cn.kennylee.banyan.excel.exporter;

import java.io.IOException;
import java.util.List;

/**
 * 行写入器，把一批数据写进导出目标的 SPI，由实现模块提供。
 *
 * <p>生命周期固定为 {@link #open(WriterContext)} → 若干次 {@link #write(List)}
 * 与 {@link #nextSheet()} → {@link #close()}。导出流程保证 {@code close} 一定被调用。</p>
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public interface ExcelRowWriter<T> extends AutoCloseable {

    /**
     * 打开写出目标。
     *
     * @param context 打开上下文
     * @throws IOException 打开失败
     */
    void open(WriterContext<T> context) throws IOException;

    /**
     * 写入一批数据，必须写入传入的全部行。
     *
     * @param rows 一批数据
     * @throws IOException 写入失败
     */
    void write(List<T> rows) throws IOException;

    /**
     * 切换到下一个 sheet；不支持多 sheet 的格式实现为空操作。
     *
     * @throws IOException 切换失败
     */
    void nextSheet() throws IOException;

    /**
     * 完成写入并释放资源。
     *
     * @throws IOException 关闭失败
     */
    @Override
    void close() throws IOException;
}
