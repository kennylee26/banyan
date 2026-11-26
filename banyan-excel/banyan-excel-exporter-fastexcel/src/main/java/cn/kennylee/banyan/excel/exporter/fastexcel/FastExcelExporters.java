package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.excel.exporter.BatchSource;
import cn.kennylee.banyan.excel.exporter.ExportFormat;
import cn.kennylee.banyan.excel.exporter.ExcelExporterBuilder;
import cn.kennylee.banyan.excel.exporter.ExcelRowWriter;

/**
 * 基于 FastExcel 的导出入口。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class FastExcelExporters {

    private FastExcelExporters() {
    }

    /**
     * 创建 xlsx 导出构建器。
     *
     * @param batchSource 批次数据源
     * @param rowType     表头类
     * @param <T>         行数据类型
     * @return 导出构建器
     */
    public static <T> ExcelExporterBuilder<T> xlsx(BatchSource<T> batchSource, Class<T> rowType) {
        return create(ExportFormat.XLSX, new FastExcelXlsxRowWriter<>(), batchSource, rowType);
    }

    /**
     * 创建 CSV 导出构建器。
     *
     * @param batchSource 批次数据源
     * @param rowType     表头类
     * @param <T>         行数据类型
     * @return 导出构建器
     */
    public static <T> ExcelExporterBuilder<T> csv(BatchSource<T> batchSource, Class<T> rowType) {
        return create(ExportFormat.CSV, new FastExcelCsvRowWriter<>(), batchSource, rowType);
    }

    /**
     * 按格式创建导出构建器。
     *
     * @param format      导出格式
     * @param batchSource 批次数据源
     * @param rowType     表头类
     * @param <T>         行数据类型
     * @return 导出构建器
     * @throws BanyanException 格式为空时抛出
     */
    public static <T> ExcelExporterBuilder<T> of(ExportFormat format, BatchSource<T> batchSource, Class<T> rowType) {
        if (format == null) {
            throw new BanyanException(ErrorCode.EXPORT_REQUIRED, "format");
        }
        return switch (format) {
            case CSV -> csv(batchSource, rowType);
            case XLSX -> xlsx(batchSource, rowType);
        };
    }

    private static <T> ExcelExporterBuilder<T> create(ExportFormat format, ExcelRowWriter<T> rowWriter,
                                                      BatchSource<T> batchSource, Class<T> rowType) {
        return ExcelExporterBuilder.of(batchSource, rowWriter, rowType).format(format);
    }
}
