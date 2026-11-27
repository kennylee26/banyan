package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.kennylee.banyan.excel.parser.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 大数据低堆测试：在 64MB 堆下流式读取 20 万行 xlsx，验证不 OOM。
 *
 * <p>低堆值由命令行 {@code -DargLine='-Xmx64m'} 传入，pom 不写死；默认堆运行时同样通过。
 * 数据文件按批生成，避免测试自身驻留全量行；读取时处理器只计数。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
class LargeParseLowHeapTest {

    private static final int TOTAL_ROWS = 200_000;

    private static final int WRITE_BATCH = 1_000;

    private static final int READ_BATCH = 1_000;

    @Test
    @DisplayName("64MB 堆下读取 20 万行不 OOM")
    void parsesTwoHundredThousandRows() {
        // given
        File file = newTempFile();
        try {
            writeInBatches(file, TOTAL_ROWS);
            AtomicInteger handled = new AtomicInteger();

            // when
            ParseResult result = FastExcelParsers.xlsx(LargeRow.class)
                .batchSize(READ_BATCH)
                .read(file, batch -> handled.addAndGet(batch.size()));

            // then
            assertEquals(TOTAL_ROWS, result.totalRows());
            assertEquals(TOTAL_ROWS, handled.get());
            assertEquals(TOTAL_ROWS / READ_BATCH, result.batchCount());
        } finally {
            file.delete();
        }
    }

    private static void writeInBatches(File file, int total) {
        try (ExcelWriter writer = FastExcel.write(file, LargeRow.class).build()) {
            WriteSheet sheet = FastExcel.writerSheet(0, "data").build();
            for (int start = 0; start < total; start += WRITE_BATCH) {
                writer.write(buildBatch(start, WRITE_BATCH), sheet);
            }
        }
    }

    private static List<LargeRow> buildBatch(int start, int size) {
        List<LargeRow> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            LargeRow row = new LargeRow();
            row.setId("ID-" + (start + i));
            row.setValue(new BigDecimal("1.00"));
            rows.add(row);
        }
        return rows;
    }

    private static File newTempFile() {
        try {
            File file = File.createTempFile("large-parse-", ".xlsx");
            file.deleteOnExit();
            return file;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("创建临时文件失败", e);
        }
    }

    /**
     * 大数据行。
     */
    public static class LargeRow {

        @ExcelProperty("编号")
        private String id;

        @ExcelProperty("值")
        private BigDecimal value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }
}
