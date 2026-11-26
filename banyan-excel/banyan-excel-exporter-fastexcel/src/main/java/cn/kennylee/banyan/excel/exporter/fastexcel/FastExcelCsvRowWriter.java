package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.kennylee.banyan.excel.exporter.WriterContext;

/**
 * 基于 FastExcel 的 CSV 行写入器，纯文本旁路，不应用模板、不拆分 sheet。
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public final class FastExcelCsvRowWriter<T> extends AbstractFastExcelRowWriter<T> {

    /** 固定写入第一个 sheet。 */
    private static final int FIRST_SHEET = 0;

    @Override
    public void open(WriterContext<T> context) {
        this.writer = FastExcel.write(context.target(), context.rowType())
            .excelType(ExcelTypeEnum.CSV)
            .needHead(true)
            .build();
        this.writeSheet = FastExcel.writerSheet(FIRST_SHEET).build();
    }

    @Override
    public void nextSheet() {
        // CSV 无多 sheet 概念，忽略切换。
    }
}
