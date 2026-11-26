package cn.kennylee.banyan.excel.exporter;

import cn.kennylee.banyan.commons.error.BanyanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导出边界测试：默认不限、上限截断、空数据、失败清理与模板流关闭。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelExporterEdgeCaseTest {

    @Test
    @DisplayName("默认不限条数时导出全部数据且不标记截断")
    void exportsAllByDefaultWithoutTruncation() {
        // given
        List<String> data = IntStream.range(0, 7).mapToObj(String::valueOf).toList();
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class).batchSize(3).export();

        // then
        assertEquals(7, result.totalRecords());
        assertFalse(result.truncated());
        assertNotNull(result.file());
        result.deleteFile();
    }

    @Test
    @DisplayName("显式上限触顶时静默截断并标记")
    void truncatesAtExplicitLimit() {
        // given
        List<String> data = IntStream.range(0, 10).mapToObj(String::valueOf).toList();
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(data);

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class)
            .batchSize(3)
            .maxExportRecords(5)
            .export();

        // then
        assertEquals(5, result.totalRecords());
        assertTrue(result.truncated());
        result.deleteFile();
    }

    @Test
    @DisplayName("空数据默认仍产出文件")
    void emptyDataStillProducesFileByDefault() {
        // given
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(List.of());

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class).export();

        // then
        assertEquals(0, result.totalRecords());
        assertNotNull(result.file());
        assertTrue(result.file().exists());
        result.deleteFile();
    }

    @Test
    @DisplayName("空数据且关闭产出时结果文件为空")
    void emptyDataWithoutFileWhenDisabled() {
        // given
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(List.of());

        // when
        ExcelExportResult result = ExcelExporterBuilder.of(source, writer, String.class)
            .writeEmptyData(false)
            .export();

        // then
        assertEquals(0, result.totalRecords());
        assertNull(result.file());
    }

    @Test
    @DisplayName("写出失败时清理临时文件并抛本库异常")
    void cleansUpAndWrapsOnFailure() {
        // given
        FailingWriter<String> writer = new FailingWriter<>();
        ListSource<String> source = new ListSource<>(List.of("a", "b"));

        // when
        BanyanException exception = assertThrows(BanyanException.class,
            () -> ExcelExporterBuilder.of(source, writer, String.class).export());

        // then
        assertEquals("banyan.excel.export-failed", exception.getErrorCode().messageKey());
        assertNotNull(writer.target);
        assertFalse(writer.target.exists(), "失败后临时文件应被清理");
        assertEquals(1, writer.closeCount, "失败时写出器仍应被关闭");
    }

    @Test
    @DisplayName("导出结束时关闭模板输入流")
    void closesTemplateStream() {
        // given
        TrackingInputStream template = new TrackingInputStream();
        RecordingWriter<String> writer = new RecordingWriter<>();
        ListSource<String> source = new ListSource<>(List.of("a"));

        // when
        ExcelExporterBuilder.of(source, writer, String.class).template(template).export();

        // then
        assertTrue(template.closed, "模板流应由导出流程关闭");
    }

    /**
     * 记录调用情况的替身写出器。
     *
     * @param <T> 行数据类型
     */
    static class RecordingWriter<T> implements ExcelRowWriter<T> {

        int closeCount;

        @Override
        public void open(WriterContext<T> context) {
        }

        @Override
        public void write(List<T> rows) throws IOException {
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
     * 写出即失败的替身写出器，记录目标文件。
     *
     * @param <T> 行数据类型
     */
    static final class FailingWriter<T> extends RecordingWriter<T> {

        File target;

        @Override
        public void open(WriterContext<T> context) {
            this.target = context.target();
        }

        @Override
        public void write(List<T> rows) throws IOException {
            throw new IOException("模拟写出失败");
        }
    }

    /**
     * 记录关闭状态的输入流。
     */
    static final class TrackingInputStream extends ByteArrayInputStream {

        boolean closed;

        TrackingInputStream() {
            super(new byte[0]);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /**
     * 按已导出行数切分的列表数据源。
     *
     * @param <T> 行数据类型
     */
    static class ListSource<T> implements BatchSource<T> {

        private final List<T> data;
        final List<ExportCursor> cursors = new ArrayList<>();

        ListSource(List<T> data) {
            this.data = data;
        }

        @Override
        public List<T> loadNextBatch(ExportCursor cursor, int batchSize) {
            cursors.add(cursor);
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
