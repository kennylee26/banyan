package cn.kennylee.banyan.excel.exporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导出公共契约测试：批次推进、逐批写出与资源关闭。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelExporterContractTest {

    @Test
    @DisplayName("多批次数据逐批写出并统计总记录数")
    void writesBatchesOneByOneAndCountsRows() {
        // given
        List<String> data = IntStream.range(0, 10).mapToObj(String::valueOf).toList();
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class)
            .batchSize(4)
            .export();

        // then
        assertEquals(10, result.totalRecords());
        assertEquals(List.of(4, 4, 2), writer.batchSizes);
        assertEquals(1, writer.openCount);
        assertEquals(1, writer.closeCount);
        assertEquals(List.of(4, 4, 4), source.requestedSizes, "库应按批次大小逐批请求，而非一次取全量");
        assertTrue(result.deleteFile());
    }

    @Test
    @DisplayName("游标在批次间推进并携带上一批末条 ID")
    void advancesCursorBetweenBatches() {
        // given
        List<String> data = IntStream.range(0, 5).mapToObj(String::valueOf).toList();
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExporterBuilder.of(source, writer, String.class).batchSize(3).export();

        // then
        assertEquals(2, source.cursors.size());
        assertEquals(new ExportCursor(0, 0, null), source.cursors.get(0));
        assertEquals(1, source.cursors.get(1).batchIndex());
        assertEquals(3, source.cursors.get(1).exportedCount());
        assertEquals("2", source.cursors.get(1).prevPageLastId());
    }

    @Test
    @DisplayName("写出器在导出结束时一定被关闭")
    void alwaysClosesWriter() {
        // given
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(List.of("a"));

        // when
        ExcelExporterBuilder.of(source, writer, String.class).export();

        // then
        assertEquals(1, writer.closeCount);
    }

    /**
     * 记录调用情况的替身写出器。
     *
     * @param <T> 行数据类型
     */
    static final class RecordingWriter<T> implements ExcelRowWriter<T> {

        final List<Integer> batchSizes = new ArrayList<>();
        int openCount;
        int closeCount;

        @Override
        public void open(WriterContext<T> context) {
            this.openCount++;
        }

        @Override
        public void write(List<T> rows) {
            batchSizes.add(rows.size());
        }

        @Override
        public void nextSheet() {
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    /**
     * 按已导出行数切分的列表数据源。
     *
     * @param <T> 行数据类型
     */
    static final class ListSource<T> implements BatchSource<T> {

        private final List<T> data;
        final List<ExportCursor> cursors = new ArrayList<>();
        final List<Integer> requestedSizes = new ArrayList<>();

        ListSource(List<T> data) {
            this.data = data;
        }

        @Override
        public List<T> loadNextBatch(ExportCursor cursor, int batchSize) {
            cursors.add(cursor);
            requestedSizes.add(batchSize);
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
