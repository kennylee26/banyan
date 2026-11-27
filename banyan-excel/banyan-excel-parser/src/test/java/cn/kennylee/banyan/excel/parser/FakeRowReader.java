package cn.kennylee.banyan.excel.parser;

import java.util.List;

/**
 * 测试替身：按预设批次推行的纯 JDK 读取器，不依赖任何 Excel 库。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class FakeRowReader<T> implements ExcelRowReader<T> {

    private final List<List<T>> batches;
    private final int firstRowIndex;

    boolean opened;
    boolean closed;
    ReaderContext<T> context;

    FakeRowReader(List<List<T>> batches) {
        this(batches, 2);
    }

    FakeRowReader(List<List<T>> batches, int firstRowIndex) {
        this.batches = batches;
        this.firstRowIndex = firstRowIndex;
    }

    @Override
    public void open(ReaderContext<T> context) {
        this.context = context;
        this.opened = true;
    }

    @Override
    public void read(RowSink<T> sink) {
        int start = firstRowIndex;
        for (List<T> batch : batches) {
            if (!sink.accept(batch, start)) {
                return;
            }
            start += batch.size();
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }
}
