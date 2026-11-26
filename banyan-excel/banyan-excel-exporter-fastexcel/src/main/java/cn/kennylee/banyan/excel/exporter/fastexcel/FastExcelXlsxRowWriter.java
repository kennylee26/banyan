package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.idev.excel.FastExcel;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.kennylee.banyan.excel.exporter.WriterContext;

/**
 * 基于 FastExcel 的 xlsx 行写入器，支持模板填充与多 sheet。
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public final class FastExcelXlsxRowWriter<T> extends AbstractFastExcelRowWriter<T> {

    /** 首个 sheet 编号。 */
    private static final int FIRST_SHEET = 0;

    /** 当前 sheet 编号。 */
    private int sheetNo;

    @Override
    public void open(WriterContext<T> context) {
        ExcelWriterBuilder builder = FastExcel.write(context.target(), context.rowType());
        if (context.template() != null) {
            builder.withTemplate(context.template()).needHead(false);
        } else {
            builder.needHead(true);
        }
        this.writer = builder.build();
        this.sheetNo = FIRST_SHEET;
        this.writeSheet = FastExcel.writerSheet(FIRST_SHEET).build();
    }

    @Override
    public void nextSheet() {
        sheetNo++;
        this.writeSheet = FastExcel.writerSheet(sheetNo).build();
    }
}
