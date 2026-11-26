package cn.kennylee.banyan.excel.exporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 自动分 sheet 测试：xlsx 触顶切换 sheet 且不重放历史批次，CSV 不拆分。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SheetSwitchTest {

    @Test
    @DisplayName("xlsx 超过 sheet 容量时自动切换 sheet")
    void switchesSheetWhenCapacityExceeded() {
        // given
        List<String> data = IntStream.range(0, 8).mapToObj(String::valueOf).toList();
        CapturingWriter<String> writer = new CapturingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class)
            .format(ExportFormat.XLSX)
            .batchSize(3)
            .sheetCapacity(3)
            .export();

        // then
        assertEquals(8, result.totalRecords());
        assertEquals(2, writer.nextSheetCount, "容量 3、共 8 行应切换 2 次");
        assertEquals(8, writer.written.stream().mapToInt(List::size).sum());
        result.deleteFile();
    }

    @Test
    @DisplayName("切换 sheet 时不重放历史批次")
    void doesNotReplayPreviousBatches() {
        // given
        List<String> data = IntStream.range(0, 8).mapToObj(String::valueOf).toList();
        CapturingWriter<String> writer = new CapturingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExporterBuilder.of(source, writer, String.class)
            .format(ExportFormat.XLSX)
            .batchSize(3)
            .sheetCapacity(3)
            .export();

        // then
        List<String> written = writer.written.stream().flatMap(List::stream).toList();
        assertEquals(data, written, "写出的数据应与源数据顺序一致且无重复");
    }

    @Test
    @DisplayName("CSV 不按 sheet 容量拆分")
    void csvDoesNotSplitBySheetCapacity() {
        // given
        List<String> data = IntStream.range(0, 8).mapToObj(String::valueOf).toList();
        CapturingWriter<String> writer = new CapturingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class)
            .format(ExportFormat.CSV)
            .batchSize(3)
            .sheetCapacity(3)
            .export();

        // then
        assertEquals(8, result.totalRecords());
        assertEquals(0, writer.nextSheetCount, "CSV 不应切换 sheet");
        result.deleteFile();
    }

    /**
     * 记录每批内容的替身写出器。
     *
     * @param <T> 行数据类型
     */
    static final class CapturingWriter<T> implements ExcelRowWriter<T> {

        final List<List<T>> written = new ArrayList<>();
        int nextSheetCount;

        @Override
        public void open(WriterContext<T> context) {
        }

        @Override
        public void write(List<T> rows) {
            written.add(new ArrayList<>(rows));
        }

        @Override
        public void nextSheet() {
            nextSheetCount++;
        }

        @Override
        public void close() {
        }
    }

    /**
     * 按已导出行数切分的列表数据源。
     *
     * @param <T> 行数据类型
     */
    static final class ListSource<T> implements BatchSource<T> {

        private final List<T> data;

        ListSource(List<T> data) {
            this.data = data;
        }

        @Override
        public List<T> loadNextBatch(ExportCursor cursor, int batchSize) {
            int from = cursor.exportedCount();
            if (from >= data.size()) {
                return List.of();
            }
            int to = Math.min(data.size(), from + batchSize);
            return new ArrayList<>(data.subList(from, to));
        }

        @Override
        public String lastIdOf(List<T> rows) {
            return rows.isEmpty() ? null : String.valueOf(rows.get(rows.size() - 1));
        }
    }
}
