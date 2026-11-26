package cn.kennylee.banyan.excel.exporter;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.io.InputStream;

/**
 * 导出构建器，收集导出配置并创建 {@link ExcelExporter}。
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public final class ExcelExporterBuilder<T> {

    /** 批次数据源。 */
    private final BatchSource<T> batchSource;

    /** 行写入器。 */
    private final ExcelRowWriter<T> rowWriter;

    /** 表头类。 */
    private final Class<T> rowType;

    /** 导出格式。 */
    private ExportFormat format = ExportFormat.XLSX;

    /** 模板输入流，可为 null。 */
    private InputStream template;

    /** 批次大小。 */
    private int batchSize = ExcelExporter.DEFAULT_BATCH_SIZE;

    /** 最大导出条数。 */
    private int maxExportRecords = ExcelExporter.UNLIMITED;

    /** 单 sheet 容量。 */
    private int sheetCapacity = ExcelExporter.DEFAULT_SHEET_CAPACITY;

    /** 空数据是否仍产出文件。 */
    private boolean writeEmptyData = true;

    private ExcelExporterBuilder(BatchSource<T> batchSource, ExcelRowWriter<T> rowWriter, Class<T> rowType) {
        this.batchSource = batchSource;
        this.rowWriter = rowWriter;
        this.rowType = rowType;
    }

    /**
     * 创建导出构建器。
     *
     * @param batchSource 批次数据源
     * @param rowWriter   行写入器
     * @param rowType     表头类
     * @param <T>         行数据类型
     * @return 导出构建器
     * @throws BanyanException 任一必需参数为空时抛出
     */
    public static <T> ExcelExporterBuilder<T> of(BatchSource<T> batchSource, ExcelRowWriter<T> rowWriter, Class<T> rowType) {
        if (batchSource == null) {
            throw new BanyanException(ErrorCode.EXPORT_REQUIRED, "batchSource");
        }
        if (rowWriter == null) {
            throw new BanyanException(ErrorCode.EXPORT_REQUIRED, "rowWriter");
        }
        if (rowType == null) {
            throw new BanyanException(ErrorCode.EXPORT_REQUIRED, "rowType");
        }
        return new ExcelExporterBuilder<>(batchSource, rowWriter, rowType);
    }

    /**
     * 设置导出格式。
     *
     * @param format 导出格式
     * @return 当前构建器
     * @throws BanyanException 格式为空时抛出
     */
    public ExcelExporterBuilder<T> format(ExportFormat format) {
        if (format == null) {
            throw new BanyanException(ErrorCode.EXPORT_REQUIRED, "format");
        }
        this.format = format;
        return this;
    }

    /**
     * 设置 xlsx 模板输入流。
     *
     * @param template 模板输入流，可为 null
     * @return 当前构建器
     */
    public ExcelExporterBuilder<T> template(InputStream template) {
        this.template = template;
        return this;
    }

    /**
     * 设置批次大小。
     *
     * @param batchSize 每批行数
     * @return 当前构建器
     */
    public ExcelExporterBuilder<T> batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * 设置最大导出条数。
     *
     * @param maxExportRecords 最大行数，{@link ExcelExporter#UNLIMITED} 表示不限
     * @return 当前构建器
     */
    public ExcelExporterBuilder<T> maxExportRecords(int maxExportRecords) {
        this.maxExportRecords = maxExportRecords;
        return this;
    }

    /**
     * 设置单 sheet 容量。
     *
     * @param sheetCapacity 每个 sheet 最多行数
     * @return 当前构建器
     */
    public ExcelExporterBuilder<T> sheetCapacity(int sheetCapacity) {
        this.sheetCapacity = sheetCapacity;
        return this;
    }

    /**
     * 设置空数据时是否仍产出文件。
     *
     * @param writeEmptyData {@code true} 表示空数据仍产出文件
     * @return 当前构建器
     */
    public ExcelExporterBuilder<T> writeEmptyData(boolean writeEmptyData) {
        this.writeEmptyData = writeEmptyData;
        return this;
    }

    /**
     * 执行导出。
     *
     * @return 导出结果
     * @throws BanyanException 配置非法或导出失败时抛出
     */
    public ExcelExportResult export() {
        if (batchSize <= 0) {
            throw new BanyanException(ErrorCode.EXPORT_CONFIG_INVALID, "batchSize=" + batchSize);
        }
        if (sheetCapacity <= 0) {
            throw new BanyanException(ErrorCode.EXPORT_CONFIG_INVALID, "sheetCapacity=" + sheetCapacity);
        }
        if (maxExportRecords == 0 || maxExportRecords < ExcelExporter.UNLIMITED) {
            throw new BanyanException(ErrorCode.EXPORT_CONFIG_INVALID, "maxExportRecords=" + maxExportRecords);
        }
        ExportOptions<T> options = new ExportOptions<>(format, rowType, template, batchSize,
            maxExportRecords, sheetCapacity, writeEmptyData);
        return new ExcelExporter<>(batchSource, rowWriter, options).export();
    }
}
