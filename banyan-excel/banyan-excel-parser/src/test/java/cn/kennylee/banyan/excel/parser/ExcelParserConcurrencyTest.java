package cn.kennylee.banyan.excel.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共解析契约测试：保序、并发度与在途批次上限。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelParserConcurrencyTest {

    @Test
    @DisplayName("保序模式按批次顺序串行回调")
    void orderedSerializesBatches() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c")));
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        List<Integer> order = new java.util.concurrent.CopyOnWriteArrayList<>();

        // when
        ExcelParserBuilder.of(reader, String.class)
            .ordered(true)
            .parallelism(4)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
                int now = inFlight.incrementAndGet();
                maxInFlight.accumulateAndGet(now, Math::max);
                order.add(batch.firstRowIndex());
                sleep(20);
                inFlight.decrementAndGet();
            });

        // then
        assertEquals(List.of(2, 3, 4), order);
        assertEquals(1, maxInFlight.get(), "保序模式不应并发处理批次");
    }

    @Test
    @DisplayName("无序模式可以并发处理多个批次")
    void unorderedRunsBatchesConcurrently() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c"), List.of("d")));
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        // when
        ExcelParserBuilder.of(reader, String.class)
            .ordered(false)
            .parallelism(4)
            .maxPendingBatches(4)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
                maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                sleep(60);
                inFlight.decrementAndGet();
            });

        // then
        assertTrue(maxInFlight.get() > 1, "无序且并发度大于 1 时应并行处理批次");
    }

    @Test
    @DisplayName("并发度为 1 时无序模式仍串行")
    void singleParallelismIsSequential() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c")));
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        // when
        ExcelParserBuilder.of(reader, String.class)
            .ordered(false)
            .parallelism(1)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
                maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                sleep(10);
                inFlight.decrementAndGet();
            });

        // then
        assertEquals(1, maxInFlight.get());
    }

    @Test
    @DisplayName("在途批次上限约束并发处理数")
    void pendingBatchesAreBounded() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c"), List.of("d")));
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        // when
        ExcelParserBuilder.of(reader, String.class)
            .ordered(false)
            .parallelism(4)
            .maxPendingBatches(1)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
                maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                sleep(30);
                inFlight.decrementAndGet();
            });

        // then
        assertEquals(1, maxInFlight.get(), "在途批次上限为 1 时不应有两个批次同时处理");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
