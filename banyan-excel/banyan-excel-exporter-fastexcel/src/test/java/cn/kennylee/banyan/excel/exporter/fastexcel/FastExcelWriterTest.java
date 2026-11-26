package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.idev.excel.FastExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.read.listener.PageReadListener;
import cn.kennylee.banyan.excel.exporter.BatchSource;
import cn.kennylee.banyan.excel.exporter.ExportCursor;
import cn.kennylee.banyan.excel.exporter.ExportFormat;
import cn.kennylee.banyan.excel.exporter.ExcelExportResult;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实现模块集成测试：真实 fastexcel 的 xlsx / CSV 写出与读回。
 *
 * @author kennylee
 * @since 0.1.0
 */
class FastExcelWriterTest {

    @Test
    @DisplayName("无模板 xlsx 写出表头与数据行并可读回")
    void writesXlsxWithoutTemplate() {
        // given
        List<Row> data = List.of(new Row("苹果", 3), new Row("香蕉", 5));

        // when
        ExcelExportResult result = FastExcelExporters.xlsx(new ListSource<>(data), Row.class).export();

        // then
        assertNotNull(result.file());
        List<Row> read = readBack(result.file());
        assertEquals(data, read);
        result.deleteFile();
    }

    @Test
    @DisplayName("带模板 xlsx 导出不重复表头")
    void writesXlsxWithTemplate() throws IOException {
        // given
        File template = createTemplateWithHeader();
        List<Row> data = List.of(new Row("苹果", 3), new Row("香蕉", 5));

        // when
        ExcelExportResult result;
        try (InputStream templateStream = Files.newInputStream(template.toPath())) {
            result = FastExcelExporters.xlsx(new ListSource<>(data), Row.class)
                .template(templateStream)
                .export();
        }

        // then
        List<Row> read = readBack(result.file());
        assertEquals(data, read, "模板已含表头，导出后不应重复表头");
        result.deleteFile();
        template.delete();
    }

    @Test
    @DisplayName("CSV 写出表头与数据行并可读回")
    void writesCsv() {
        // given
        List<Row> data = List.of(new Row("苹果", 3), new Row("香蕉", 5));

        // when
        ExcelExportResult result = FastExcelExporters.csv(new ListSource<>(data), Row.class).export();

        // then
        assertNotNull(result.file());
        List<Row> read = readBack(result.file());
        assertEquals(data, read);
        result.deleteFile();
    }

    @Test
    @DisplayName("CSV 忽略传入模板")
    void csvIgnoresTemplate() throws IOException {
        // given
        File template = createTemplateWithHeader();
        List<Row> data = List.of(new Row("苹果", 3));
        ListSource<Row> source = new ListSource<>(data);

        // when
        ExcelExportResult result;
        try (InputStream templateStream = Files.newInputStream(template.toPath())) {
            result = FastExcelExporters.of(ExportFormat.CSV, source, Row.class)
                .template(templateStream)
                .export();
        }

        // then
        assertEquals(data, readBack(result.file()));
        result.deleteFile();
        template.delete();
    }

    @Test
    @DisplayName("空数据仍产出可读文件")
    void emptyDataProducesReadableFile() {
        // given
        ListSource<Row> source = new ListSource<>(List.of());

        // when
        ExcelExportResult result = FastExcelExporters.xlsx(source, Row.class).export();

        // then
        assertNotNull(result.file());
        assertTrue(result.file().length() > 0, "空数据也应产出非空文件");
        assertEquals(0, readBack(result.file()).size());
        result.deleteFile();
    }

    private static List<Row> readBack(File file) {
        List<Row> rows = new ArrayList<>();
        FastExcel.read(file, Row.class, new PageReadListener<Row>(rows::addAll)).sheet().doReadSync();
        return rows;
    }

    private static File createTemplateWithHeader() throws IOException {
        File template = File.createTempFile("template-", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(template)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("名称");
            header.createCell(1).setCellValue("数量");
            workbook.write(out);
        }
        return template;
    }

    /**
     * 导出测试用行数据。
     *
     * @author kennylee
     * @since 0.1.0
     */
    public static class Row {

        @ExcelProperty("名称")
        private String name;

        @ExcelProperty("数量")
        private Integer count;

        public Row() {
        }

        public Row(String name, Integer count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Row that)) {
                return false;
            }
            return java.util.Objects.equals(name, that.name) && java.util.Objects.equals(count, that.count);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, count);
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
    }
}
