package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.kennylee.banyan.excel.exporter.BatchSource;
import cn.kennylee.banyan.excel.exporter.ExportCursor;
import cn.kennylee.banyan.excel.exporter.ExportFormat;
import cn.kennylee.banyan.excel.exporter.ExcelExportResult;
import cn.kennylee.banyan.excel.exporter.ExcelExporterBuilder;
import cn.kennylee.banyan.excel.exporter.ExcelRowWriter;
import cn.kennylee.banyan.excel.exporter.WriterContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 大数据量导出测试：按需生成数据源、逐批写出，验证不因全量驻留而内存溢出。
 *
 * <p>低堆验证须以命令行 {@code -DargLine='-Xmx64m'} 运行，不写死进 pom。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
class LargeExportLowHeapTest {

    /** xlsx 导出行数。 */
    private static final int XLSX_ROWS = 200_000;

    /** CSV 导出行数。 */
    private static final int CSV_ROWS = 100_000;

    /** 批次大小。 */
    private static final int BATCH_SIZE = 1_000;

    /** xlsx 单 sheet 容量，故意设小以触发自动分 sheet。 */
    private static final int SHEET_CAPACITY = 50_000;

    @Test
    @DisplayName("大数据量 xlsx 导出完成并自动分 sheet")
    void exportsLargeXlsxAndSplitsSheets() {
        // given
        CountingXlsxWriter<FastExcelWriterTest.Row> writer = new CountingXlsxWriter<>();
        GeneratingSource source = new GeneratingSource(XLSX_ROWS);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, FastExcelWriterTest.Row.class)
            .format(ExportFormat.XLSX)
            .batchSize(BATCH_SIZE)
            .sheetCapacity(SHEET_CAPACITY)
            .export();

        // then
        assertEquals(XLSX_ROWS, result.totalRecords());
        assertEquals(XLSX_ROWS / SHEET_CAPACITY - 1, writer.nextSheetCount, "应按容量自动切换 sheet");
        assertTrue(result.file() != null && result.file().exists());
        result.deleteFile();
    }

    @Test
    @DisplayName("大数据量 CSV 导出完成且不拆分")
    void exportsLargeCsv() {
        // given
        GeneratingSource source = new GeneratingSource(CSV_ROWS);

        // when
        ExcelExportResult result = FastExcelExporters.csv(source, FastExcelWriterTest.Row.class)
            .batchSize(BATCH_SIZE)
            .export();

        // then
        assertEquals(CSV_ROWS, result.totalRecords());
        assertTrue(result.file() != null && result.file().getName().endsWith(".csv"));
        result.deleteFile();
    }

    /**
     * 按需生成行、用完即弃的数据源，不持有全量数据。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static final class GeneratingSource implements BatchSource<FastExcelWriterTest.Row> {

        private final int total;

        GeneratingSource(int total) {
            this.total = total;
        }

        @Override
        public List<FastExcelWriterTest.Row> loadNextBatch(ExportCursor cursor, int batchSize) {
            int from = cursor.exportedCount();
            if (from >= total) {
                return List.of();
            }
            int to = Math.min(total, from + batchSize);
            List<FastExcelWriterTest.Row> rows = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                rows.add(new FastExcelWriterTest.Row("row-" + i, i));
            }
            return rows;
        }
    }

    /**
     * 包装真实 xlsx 写入器并统计 sheet 切换次数。
     *
     * @param <T> 行数据类型
     */
    static final class CountingXlsxWriter<T> implements ExcelRowWriter<T> {

        private final FastExcelXlsxRowWriter<T> delegate = new FastExcelXlsxRowWriter<>();

        int nextSheetCount;

        @Override
        public void open(WriterContext<T> context) {
            delegate.open(context);
        }

        @Override
        public void write(List<T> rows) throws IOException {
            delegate.write(rows);
        }

        @Override
        public void nextSheet() throws IOException {
            nextSheetCount++;
            delegate.nextSheet();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
