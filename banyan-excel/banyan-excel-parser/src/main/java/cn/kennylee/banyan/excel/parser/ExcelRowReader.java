package cn.kennylee.banyan.excel.parser;

import java.io.IOException;

/**
 * 行读取器 SPI：把输入源解码成目标类型的行，并按批次推给公共层。
 *
 * <p>实现模块只负责解码与攒批，不感知批次处理器、并发度与保序；这些由公共层的解析流程处理。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
public interface ExcelRowReader<T> extends AutoCloseable {

    /**
     * 打开读取器。
     *
     * @param context 读取上下文
     * @throws IOException 打开输入源失败
     */
    void open(ReaderContext<T> context) throws IOException;

    /**
     * 读取全部行并推给接收端。
     *
     * @param sink 批次接收端
     * @throws IOException 读取失败
     */
    void read(RowSink<T> sink) throws IOException;

    /**
     * 释放读取器资源。
     *
     * @throws IOException 关闭失败
     */
    @Override
    void close() throws IOException;
}
