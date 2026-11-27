package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.kennylee.banyan.excel.parser.ParseResult;
import cn.kennylee.banyan.excel.parser.SheetRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 500 万行探针：默认跳过，显式传 {@code -Dsamples.excel.parser.large=true} 才执行。
 *
 * <p>xlsx 单 sheet 物理上限约 104 万行，故按 5 个 sheet 各 100 万行生成，逐 sheet 流式读取；
 * 跑完删除临时文件，不进常规测试套件。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@EnabledIfSystemProperty(named = "samples.excel.parser.large", matches = "true")
class LargeParseProbe {

    private static final int SHEETS = 5;

    private static final int ROWS_PER_SHEET = 1_000_000;

    private static final int WRITE_BATCH = 10_000;

    private static final int READ_BATCH = 5_000;

    private static final System.Logger LOGGER = System.getLogger(LargeParseProbe.class.getName());

    @Test
    @DisplayName("500 万行流式读取（5 个 sheet，手动执行）")
    void parsesFiveMillionRows() {
        File file = newTempFile();
        try {
            writeInBatches(file);
            AtomicLong handled = new AtomicLong();
            long start = System.currentTimeMillis();

            for (int sheetNo = 0; sheetNo < SHEETS; sheetNo++) {
                ParseResult result = FastExcelParsers.xlsx(LargeParseLowHeapTest.LargeRow.class)
                    .batchSize(READ_BATCH)
                    .sheet(SheetRef.ofIndex(sheetNo))
                    .read(file, batch -> handled.addAndGet(batch.size()));
                assertEquals(ROWS_PER_SHEET, result.totalRows());
            }

            long cost = System.currentTimeMillis() - start;
            long heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long peakMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            LOGGER.log(System.Logger.Level.INFO, () -> String.format(
                "[parse-probe] rows=%d sheets=%d costMs=%d fileMB=%d maxHeapMB=%d peakUsedMB=%d",
                handled.get(), SHEETS, cost, file.length() / (1024 * 1024), heapMb, peakMb));
            assertEquals((long) SHEETS * ROWS_PER_SHEET, handled.get());
        } finally {
            file.delete();
        }
    }

    private static void writeInBatches(File file) {
        try (ExcelWriter writer = FastExcel.write(file, LargeParseLowHeapTest.LargeRow.class).build()) {
            for (int sheetNo = 0; sheetNo < SHEETS; sheetNo++) {
                WriteSheet sheet = FastExcel.writerSheet(sheetNo, "data-" + sheetNo).build();
                for (int start = 0; start < ROWS_PER_SHEET; start += WRITE_BATCH) {
                    writer.write(buildBatch(start), sheet);
                }
            }
        }
    }

    private static List<LargeParseLowHeapTest.LargeRow> buildBatch(int start) {
        List<LargeParseLowHeapTest.LargeRow> rows = new ArrayList<>(WRITE_BATCH);
        for (int i = 0; i < WRITE_BATCH; i++) {
            LargeParseLowHeapTest.LargeRow row = new LargeParseLowHeapTest.LargeRow();
            row.setId("ID-" + (start + i));
            row.setValue(new BigDecimal("1.00"));
            rows.add(row);
        }
        return rows;
    }

    private static File newTempFile() {
        try {
            File file = File.createTempFile("large-parse-probe-", ".xlsx");
            file.deleteOnExit();
            return file;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("创建临时文件失败", e);
        }
    }
}
