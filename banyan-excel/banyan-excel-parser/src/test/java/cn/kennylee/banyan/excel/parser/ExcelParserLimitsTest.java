package cn.kennylee.banyan.excel.parser;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共解析契约测试：最大行数截断、sheet 选择、表头行数、全量读取与输入流关闭责任。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelParserLimitsTest {

    @Test
    @DisplayName("达到最大行数时截断并标记")
    void truncatesAtMaxRows() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a", "b", "c"), List.of("d", "e")));
        List<String> received = new ArrayList<>();

        // when
        ParseResult result = ExcelParserBuilder.of(reader, String.class)
            .maxRows(4)
            .read(new ByteArrayInputStream(new byte[0]), batch -> received.addAll(batch.rows()));

        // then
        assertEquals(List.of("a", "b", "c", "d"), received);
        assertEquals(4, result.totalRows());
        assertTrue(result.truncated());
    }

    @Test
    @DisplayName("未设置最大行数时不截断")
    void unlimitedByDefault() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a", "b")));

        // when
        ParseResult result = ExcelParserBuilder.of(reader, String.class)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
            });

        // then
        assertEquals(2, result.totalRows());
        assertFalse(result.truncated());
    }

    @Test
    @DisplayName("后设置的 sheet 名覆盖先前的索引")
    void sheetNameOverridesIndex() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when
        ExcelParserBuilder.of(reader, String.class)
            .sheet(SheetRef.ofIndex(1))
            .sheet(SheetRef.ofName("订单"))
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
            });

        // then
        assertEquals("订单", reader.context.sheet().name());
    }

    @Test
    @DisplayName("表头行数透传给读取器")
    void passesHeadRowNumber() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when
        ExcelParserBuilder.of(reader, String.class)
            .headRowNumber(2)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
            });

        // then
        assertEquals(2, reader.context.headRowNumber());
    }

    @Test
    @DisplayName("全量读取返回所有行且保持顺序")
    void readAllReturnsAllRows() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a", "b"), List.of("c")));

        // when
        List<String> rows = ExcelParserBuilder.of(reader, String.class)
            .readAll(new ByteArrayInputStream(new byte[0]));

        // then
        assertEquals(List.of("a", "b", "c"), rows);
    }

    @Test
    @DisplayName("调用方传入的输入流不被库关闭")
    void doesNotCloseCallerStream() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));
        TrackingInputStream source = new TrackingInputStream();

        // when
        ExcelParserBuilder.of(reader, String.class).read(source, batch -> {
        });

        // then
        assertFalse(source.closed, "库不应关闭调用方传入的输入流");
    }

    @Test
    @DisplayName("在途批次上限小于 1 时拒绝配置")
    void rejectsPendingBatchesBelowOne() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(List.of("a")));

        // when
        BanyanException exception = assertThrows(BanyanException.class,
            () -> ExcelParserBuilder.of(reader, String.class)
                .maxPendingBatches(-1)
                .read(new ByteArrayInputStream(new byte[0]), batch -> {
                }));

        // then
        assertEquals(ErrorCode.PARSE_CONFIG_INVALID, exception.getErrorCode());
    }

    /**
     * 记录关闭状态的输入流。
     */
    private static final class TrackingInputStream extends InputStream {

        private boolean closed;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
