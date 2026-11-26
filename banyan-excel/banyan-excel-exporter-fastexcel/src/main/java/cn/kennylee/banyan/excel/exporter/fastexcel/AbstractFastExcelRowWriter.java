package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.kennylee.banyan.excel.exporter.ExcelRowWriter;

import java.io.IOException;
import java.util.List;

/**
 * FastExcel 行写入器公共骨架：持有底层写入器，提供逐批写入与关闭。
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
abstract class AbstractFastExcelRowWriter<T> implements ExcelRowWriter<T> {

    /** 底层写入器。 */
    protected ExcelWriter writer;

    /** 当前 sheet。 */
    protected WriteSheet writeSheet;

    @Override
    public void write(List<T> rows) {
        writer.write(rows, writeSheet);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
