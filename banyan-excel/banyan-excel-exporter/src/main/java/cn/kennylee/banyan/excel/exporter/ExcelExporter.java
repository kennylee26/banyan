package cn.kennylee.banyan.excel.exporter;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出流程，按批次从数据源取数并逐批交给行写入器，最终返回同步导出结果。
 *
 * <p>实例由 {@link ExcelExporterBuilder} 创建，不直接 new。</p>
 *
 * @param <T> 行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public final class ExcelExporter<T> {

    /** 日志器。 */
    private static final System.Logger LOGGER = System.getLogger(ExcelExporter.class.getName());

    /** 默认批次大小。 */
    public static final int DEFAULT_BATCH_SIZE = 500;

    /** 默认单 sheet 数据行容量：xlsx 物理上限 1,048,576 行，留 1 行给表头。 */
    public static final int DEFAULT_SHEET_CAPACITY = 1_048_575;

    /** 表示不限制导出条数。 */
    public static final int UNLIMITED = -1;

    /** 批次数据源。 */
    private final BatchSource<T> batchSource;

    /** 行写入器。 */
    private final ExcelRowWriter<T> rowWriter;

    /** 不可变导出配置。 */
    private final ExportOptions<T> options;

    ExcelExporter(BatchSource<T> batchSource, ExcelRowWriter<T> rowWriter, ExportOptions<T> options) {
        this.batchSource = batchSource;
        this.rowWriter = rowWriter;
        this.options = options;
    }

    /**
     * 执行同步导出。
     *
     * @return 导出结果
     * @throws BanyanException 导出失败时抛出
     */
    public ExcelExportResult export() {
        Instant start = Instant.now();
        File file = null;
        try {
            file = File.createTempFile("exported-", options.format().suffix());
            int exported;
            boolean truncated;
            try (InputStream templateStream = options.template()) {
                ExcelRowWriter<T> writer = this.rowWriter;
                try (writer) {
                    writer.open(new WriterContext<>(file, options.rowType(), templateStream));
                    Outcome outcome = writeBatches();
                    exported = outcome.exported();
                    truncated = outcome.truncated();
                }
            }
            Duration spentTime = Duration.between(start, Instant.now());
            if (!options.writeEmptyData() && exported == 0) {
                deleteQuietly(file);
                return new ExcelExportResult(null, 0, spentTime, truncated);
            }
            return new ExcelExportResult(file, exported, spentTime, truncated);
        } catch (BanyanException e) {
            deleteQuietly(file);
            throw e;
        } catch (Exception e) {
            deleteQuietly(file);
            throw new BanyanException(ErrorCode.EXPORT_FAILED, e);
        }
    }

    /**
     * 按批次循环取数并写出。
     *
     * @return 写出统计
     * @throws IOException 写出失败
     */
    private Outcome writeBatches() throws IOException {
        int exported = 0;
        int sheetRows = 0;
        int batchIndex = 0;
        String lastId = null;
        boolean truncated = false;
        while (true) {
            int remaining = options.maxExportRecords() > 0
                ? options.maxExportRecords() - exported
                : Integer.MAX_VALUE;
            if (remaining <= 0) {
                truncated = true;
                break;
            }
            int requestSize = Math.min(options.batchSize(), remaining);
            ExportCursor cursor = new ExportCursor(batchIndex, exported, lastId);
            List<T> rows = batchSource.loadNextBatch(cursor, requestSize);
            if (rows == null || rows.isEmpty()) {
                if (options.writeEmptyData() && exported == 0) {
                    // 空数据也要建立 sheet 与表头，避免产出不可读的空工作簿。
                    rowWriter.write(List.of());
                }
                break;
            }
            if (rows.size() > requestSize) {
                rows = new ArrayList<>(rows.subList(0, requestSize));
            }
            sheetRows = writeRows(rows, sheetRows);
            exported += rows.size();
            lastId = batchSource.lastIdOf(rows);
            batchIndex++;
            if (rows.size() < requestSize) {
                break;
            }
        }
        return new Outcome(exported, truncated);
    }

    /**
     * 按 sheet 容量切分并写出；不支持多 sheet 的格式直接写出。
     *
     * @param rows             一批数据
     * @param currentSheetRows 当前 sheet 已写行数
     * @return 更新后的当前 sheet 已写行数
     * @throws IOException 写出失败
     */
    private int writeRows(List<T> rows, int currentSheetRows) throws IOException {
        int sheetRows = currentSheetRows;
        if (!options.format().supportsMultipleSheets()) {
            rowWriter.write(rows);
            return sheetRows;
        }
        int offset = 0;
        while (offset < rows.size()) {
            if (sheetRows >= options.sheetCapacity()) {
                rowWriter.nextSheet();
                sheetRows = 0;
            }
            int chunk = Math.min(options.sheetCapacity() - sheetRows, rows.size() - offset);
            rowWriter.write(rows.subList(offset, offset + chunk));
            sheetRows += chunk;
            offset += chunk;
        }
        return sheetRows;
    }

    /**
     * 静默删除临时文件，删除失败只记 WARN。
     *
     * @param file 待删除文件，可为 {@code null}
     */
    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            LOGGER.log(System.Logger.Level.WARNING, "临时文件删除失败: {0}", file.getAbsolutePath());
        }
    }

    /**
     * 写出统计。
     *
     * @param exported  已导出行数
     * @param truncated 是否触顶截断
     */
    private record Outcome(int exported, boolean truncated) {
    }
}
