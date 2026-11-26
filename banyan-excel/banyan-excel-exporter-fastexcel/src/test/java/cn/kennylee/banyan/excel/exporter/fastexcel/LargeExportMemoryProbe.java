package cn.kennylee.banyan.excel.exporter.fastexcel;

import cn.kennylee.banyan.excel.exporter.ExcelExportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地峰值内存冒烟：类名不以 Test 结尾，默认不进常规套件。
 *
 * <p>需要时手动执行：{@code mvn -pl ... -Dtest=LargeExportMemoryProbe -DargLine='-Xmx64m' test}，
 * 打印峰值已用内存作为证据。这里只记录数字，不做阈值断言，避免依赖机器而 flaky。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
class LargeExportMemoryProbe {

    @Test
    @DisplayName("采样大数据导出过程的峰值已用内存")
    void probePeakMemory() throws InterruptedException {
        // given
        AtomicLong peakBytes = new AtomicLong(usedBytes());
        AtomicBoolean running = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> {
            while (running.get()) {
                peakBytes.accumulateAndGet(usedBytes(), Math::max);
                try {
                    Thread.sleep(5L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "memory-sampler");
        sampler.setDaemon(true);

        // when
        sampler.start();
        ExcelExportResult xlsx = FastExcelExporters.xlsx(
                new LargeExportLowHeapTest.GeneratingSource(200_000), FastExcelWriterTest.Row.class)
            .batchSize(1_000)
            .sheetCapacity(50_000)
            .export();
        ExcelExportResult csv = FastExcelExporters.csv(
                new LargeExportLowHeapTest.GeneratingSource(100_000), FastExcelWriterTest.Row.class)
            .batchSize(1_000)
            .export();
        running.set(false);
        sampler.join(1_000L);

        // then
        System.out.println("[probe] xlsx rows=" + xlsx.totalRecords()
            + ", csv rows=" + csv.totalRecords()
            + ", max heap(MB)=" + Runtime.getRuntime().maxMemory() / 1024 / 1024
            + ", peak used(MB)=" + peakBytes.get() / 1024 / 1024);
        xlsx.deleteFile();
        csv.deleteFile();
    }

    private static long usedBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
