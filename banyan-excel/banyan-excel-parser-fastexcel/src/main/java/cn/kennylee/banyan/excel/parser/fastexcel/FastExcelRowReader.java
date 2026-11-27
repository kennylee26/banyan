package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.idev.excel.ExcelReader;
import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.builder.ExcelReaderBuilder;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.read.metadata.ReadSheet;
import cn.idev.excel.read.metadata.holder.ReadRowHolder;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.excel.parser.ExcelRowReader;
import cn.kennylee.banyan.excel.parser.ParseFormat;
import cn.kennylee.banyan.excel.parser.ReaderContext;
import cn.kennylee.banyan.excel.parser.ReadBatch;
import cn.kennylee.banyan.excel.parser.RowError;
import cn.kennylee.banyan.excel.parser.RowSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 FastExcel 的行读取器：按表头建立列到字段的映射，逐行解码后推给公共层。
 *
 * <p>解析本身单线程串行；并发与背压由公共层处理。不关闭调用方传入的输入流。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
public final class FastExcelRowReader<T> implements ExcelRowReader<T> {

    private ExcelReader reader;

    private ReadSheet readSheet;

    private SinkListener<T> listener;

    @Override
    public void open(ReaderContext<T> context) throws IOException {
        this.listener = new SinkListener<>(context);
        ExcelReaderBuilder builder = FastExcel.read(context.source())
            .excelType(toType(context.format()))
            .headRowNumber(context.headRowNumber())
            .ignoreEmptyRow(context.ignoreEmptyRow())
            .autoCloseStream(false)
            .registerReadListener(listener);
        if (context.format() == ParseFormat.CSV) {
            builder.charset(StandardCharsets.UTF_8);
        }
        this.reader = builder.build();
        this.readSheet = new ReadSheet(context.sheet().index(), context.sheet().name());
    }

    @Override
    public void read(RowSink<T> sink) {
        listener.bindSink(sink);
        reader.read(readSheet);
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private static ExcelTypeEnum toType(ParseFormat format) {
        return switch (format) {
            case XLSX -> ExcelTypeEnum.XLSX;
            case CSV -> ExcelTypeEnum.CSV;
        };
    }

    /**
     * 把 FastExcel 的逐行回调转成批次推进。
     */
    private static final class SinkListener<T> implements ReadListener<Map<Integer, String>> {

        private final ReaderContext<T> context;

        private RowMapper<T> mapper;

        private RowSink<T> sink;

        private final List<T> buffer = new ArrayList<>();

        private int bufferFirstRowIndex;

        private volatile boolean stopped;

        SinkListener(ReaderContext<T> context) {
            this.context = context;
        }

        void bindSink(RowSink<T> sink) {
            this.sink = sink;
        }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext analysisContext) {
            this.mapper = RowMapper.create(context.rowType(), headMap, context.headerAliases());
        }

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext analysisContext) {
            if (stopped) {
                return;
            }
            if (mapper == null) {
                throw new BanyanException(ErrorCode.PARSE_FAILED,
                    new IllegalStateException("未读取到表头，无法建立列映射"));
            }
            int rowIndex = rowIndex(analysisContext);
            T mapped;
            try {
                mapped = mapper.map(row);
            } catch (RuntimeException e) {
                handleConvertError(rowIndex, e);
                return;
            }
            if (buffer.isEmpty()) {
                bufferFirstRowIndex = rowIndex;
            }
            buffer.add(mapped);
            if (buffer.size() >= context.batchSize()) {
                flush();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
            flush();
        }

        private void flush() {
            if (buffer.isEmpty()) {
                return;
            }
            List<T> batch = List.copyOf(buffer);
            buffer.clear();
            if (!sink.accept(batch, bufferFirstRowIndex)) {
                stopped = true;
            }
        }

        @Override
        public boolean hasNext(AnalysisContext analysisContext) {
            return !stopped;
        }

        @Override
        public void onException(Exception exception, AnalysisContext analysisContext) {
            if (context.onConvertError() == null) {
                throw new BanyanException(ErrorCode.PARSE_FAILED, exception);
            }
            context.onConvertError().accept(
                new RowError(rowIndex(analysisContext), exception.getMessage(), exception));
        }

        private void handleConvertError(int rowIndex, RuntimeException e) {
            if (context.onConvertError() == null) {
                throw new BanyanException(ErrorCode.PARSE_FAILED, e);
            }
            context.onConvertError().accept(new RowError(rowIndex, e.getMessage(), e));
        }

        private static int rowIndex(AnalysisContext analysisContext) {
            ReadRowHolder holder = analysisContext.readRowHolder();
            return holder == null || holder.getRowIndex() == null ? 0 : holder.getRowIndex() + 1;
        }
    }
}
