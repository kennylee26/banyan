package cn.kennylee.banyan.excel.parser;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共解析契约测试：处理器异常与转换失败的处理策略。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ExcelParserErrorTest {

    @Test
    @DisplayName("处理器异常默认终止读取并原样抛出")
    void handlerFailureTerminatesByDefault() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c")));
        AtomicInteger handled = new AtomicInteger();

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> ExcelParserBuilder.of(reader, String.class)
                .read(new ByteArrayInputStream(new byte[0]), batch -> {
                    if (handled.incrementAndGet() == 2) {
                        throw new IllegalStateException("boom");
                    }
                }));

        // then
        assertEquals("boom", exception.getMessage());
        assertEquals(2, handled.get(), "异常后不应再回调后续批次");
        assertTrue(reader.closed, "失败时读取器仍需关闭");
    }

    @Test
    @DisplayName("配置错误回调后处理器异常不终止读取")
    void handlerFailureContinuesWhenOnErrorConfigured() {
        // given
        FakeRowReader<String> reader = new FakeRowReader<>(List.of(
            List.of("a"), List.of("b"), List.of("c")));
        AtomicInteger handled = new AtomicInteger();
        List<RowError> errors = new ArrayList<>();

        // when
        ParseResult result = ExcelParserBuilder.of(reader, String.class)
            .onError(errors::add)
            .read(new ByteArrayInputStream(new byte[0]), batch -> {
                if (handled.incrementAndGet() == 2) {
                    throw new IllegalStateException("boom");
                }
            });

        // then
        assertEquals(3, handled.get());
        assertEquals(1, errors.size());
        assertEquals(1, result.errors().size());
    }

    @Test
    @DisplayName("转换失败默认抛解析失败异常")
    void convertFailureFailsFastByDefault() {
        // given
        ExcelRowReader<String> reader = new ConvertingFakeReader(false);

        // when
        BanyanException exception = assertThrows(BanyanException.class,
            () -> ExcelParserBuilder.of(reader, String.class)
                .read(new ByteArrayInputStream(new byte[0]), batch -> {
                }));

        // then
        assertEquals(ErrorCode.PARSE_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("配置转换错误回调后跳过失败行并收集")
    void convertFailureCollectedWhenConfigured() {
        // given
        ExcelRowReader<String> reader = new ConvertingFakeReader(true);
        List<String> received = new ArrayList<>();

        // when
        ParseResult result = ExcelParserBuilder.of(reader, String.class)
            .onConvertError(error -> {
            })
            .read(new ByteArrayInputStream(new byte[0]), batch -> received.addAll(batch.rows()));

        // then
        assertEquals(List.of("ok"), received);
        assertEquals(1, result.errors().size());
        assertEquals(2, result.errors().get(0).rowIndex());
        assertEquals(1, result.totalRows());
    }

    /**
     * 模拟实现模块的转换失败语义：未配置回调时抛出解析失败，配置后上报并跳过。
     */
    private static final class ConvertingFakeReader implements ExcelRowReader<String> {

        private final boolean continueOnError;
        private ReaderSinkHolder holder;

        ConvertingFakeReader(boolean continueOnError) {
            this.continueOnError = continueOnError;
        }

        @Override
        public void open(ReaderContext<String> context) {
            this.holder = new ReaderSinkHolder(context);
        }

        @Override
        public void read(RowSink<String> sink) {
            if (continueOnError) {
                holder.context.onConvertError().accept(
                    new RowError(2, "bad cell", new IllegalArgumentException("bad")));
                sink.accept(List.of("ok"), 3);
                return;
            }
            throw new BanyanException(ErrorCode.PARSE_FAILED, new IllegalArgumentException("bad"));
        }

        @Override
        public void close() {
        }
    }

    private record ReaderSinkHolder(ReaderContext<String> context) {
    }
}
