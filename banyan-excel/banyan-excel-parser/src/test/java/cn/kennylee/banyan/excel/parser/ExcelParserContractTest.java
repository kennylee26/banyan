package cn.kennylee.banyan.excel.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共解析契约测试：批次回调、行号、统计、不可变批次与读取器关闭。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelParserContractTest {

    @Test
    @DisplayName("按批次回调并统计总行数与批次数")
    void readsBatchesAndCounts() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a", "b"), List.of("c", "d"), List.of("e")));
        List<ReadBatch<String>> received = new ArrayList<>();

        // when
        ParseResult result = ExcelParserBuilder.of(reader, String.class)
            .batchSize(2)
            .read(new ByteArrayInputStream(new byte[0]), received::add);

        // then
        assertEquals(5, result.totalRows());
        assertEquals(3, result.batchCount());
        assertEquals(List.of(2, 4, 6), received.stream().map(ReadBatch::firstRowIndex).toList());
        assertTrue(reader.closed, "读取器必须被关闭");
    }

    @Test
    @DisplayName("批次数据不可变")
    void batchRowsAreImmutable() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when / then
        ExcelParserBuilder.of(reader, String.class)
            .read(new ByteArrayInputStream(new byte[0]), batch ->
                assertThrows(UnsupportedOperationException.class, () -> batch.rows().add("x")));
    }

    @Test
    @DisplayName("批次大小透传给读取器")
    void passesBatchSizeToReader() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when
        ExcelParserBuilder.of(reader, String.class)
            .batchSize(123)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
            });

        // then
        assertEquals(123, reader.context.batchSize());
    }

    @Test
    @DisplayName("读取器在读取结束后关闭")
    void closesReader() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when
        ExcelParserBuilder.of(reader, String.class)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
            });

        // then
        assertTrue(reader.opened, "读取器必须先被打开");
        assertTrue(reader.closed, "读取器必须被关闭");
    }
}
