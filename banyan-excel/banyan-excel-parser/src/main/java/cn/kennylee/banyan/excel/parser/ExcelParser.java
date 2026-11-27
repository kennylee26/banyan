package cn.kennylee.banyan.excel.parser;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 解析流程模板：打开读取器、接收批次、计数与截断、按保序规则派发处理器、组装结果。
 *
 * <p>由 {@link ExcelParserBuilder} 构造；实现模块只提供 {@link ExcelRowReader}，不感知并发与错误策略。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
public final class ExcelParser<T> {

    /** 等待在途批次结束的上限，仅作为防御性兜底。 */
    private static final long AWAIT_TIMEOUT_HOURS = 1L;

    private static final System.Logger LOGGER = System.getLogger(ExcelParser.class.getName());

    private final ExcelRowReader<T> reader;

    private final ReadOptions<T> options;

    ExcelParser(ExcelRowReader<T> reader, ReadOptions<T> options) {
        this.reader = reader;
        this.options = options;
    }

    /**
     * 读取输入流并把批次交给处理器。
     *
     * <p>库不关闭调用方传入的输入流。</p>
     *
     * @param source  输入流
     * @param handler 批次处理器
     * @return 解析结果
     * @throws BanyanException 读取失败或配置非法
     */
    public ParseResult read(InputStream source, BatchHandler<T> handler) {
        if (source == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "source");
        }
        if (handler == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "handler");
        }
        Instant start = Instant.now();
        ParseState<T> state = new ParseState<>(options, handler);
        try {
            reader.open(context(state, source));
            reader.read(state);
            state.awaitCompletion();
        } catch (RuntimeException e) {
            state.abort();
            RuntimeException handlerFailure = state.handlerFailure();
            if (handlerFailure != null) {
                throw handlerFailure;
            }
            if (e instanceof BanyanException banyanException) {
                throw banyanException;
            }
            throw new BanyanException(ErrorCode.PARSE_FAILED, e);
        } catch (Exception e) {
            state.abort();
            throw new BanyanException(ErrorCode.PARSE_FAILED, e);
        } finally {
            state.shutdown();
            closeReader();
        }
        return new ParseResult(state.totalRows(), state.batchCount(),
            Duration.between(start, Instant.now()), state.truncated(), state.errors());
    }

    private ReaderContext<T> context(ParseState<T> state, InputStream source) {
        Consumer<RowError> configured = options.onConvertError();
        Consumer<RowError> convertError = configured == null ? null : error -> {
            state.addError(error);
            configured.accept(error);
        };
        return new ReaderContext<>(source, options.rowType(), options.format(), options.sheet(),
            options.headRowNumber(), options.batchSize(), options.ignoreEmptyRow(),
            options.headerAliases(), convertError);
    }

    private void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "关闭 Excel 读取器失败", e);
        }
    }

    /**
     * 一次解析的运行状态，同时作为批次接收端。
     */
    private static final class ParseState<T> implements RowSink<T> {

        private final ReadOptions<T> options;

        private final BatchHandler<T> handler;

        private final ExecutorService executor;

        private final Semaphore permits;

        private final AtomicInteger totalRows = new AtomicInteger();

        private final AtomicInteger batchCount = new AtomicInteger();

        private final List<RowError> errors = new CopyOnWriteArrayList<>();

        private final AtomicReference<RuntimeException> handlerFailure = new AtomicReference<>();

        private final AtomicReference<RuntimeException> workerFailure = new AtomicReference<>();

        private volatile boolean truncated;

        private volatile boolean stopped;

        ParseState(ReadOptions<T> options, BatchHandler<T> handler) {
            this.options = options;
            this.handler = handler;
            if (!options.ordered() && options.parallelism() > 1) {
                AtomicInteger index = new AtomicInteger();
                this.executor = Executors.newFixedThreadPool(options.parallelism(), runnable -> {
                    Thread thread = new Thread(runnable, "excel-parser-" + index.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
                this.permits = new Semaphore(options.maxPendingBatches());
            } else {
                this.executor = null;
                this.permits = null;
            }
        }

        @Override
        public boolean accept(List<T> rows, int firstRowIndex) {
            if (stopped) {
                return false;
            }
            if (rows.isEmpty()) {
                return true;
            }
            List<T> batchRows = truncate(rows);
            if (batchRows.isEmpty()) {
                return false;
            }
            totalRows.addAndGet(batchRows.size());
            batchCount.incrementAndGet();
            boolean reached = reachedMaxRows();
            if (reached) {
                truncated = true;
            }
            ReadBatch<T> batch = new ReadBatch<>(batchRows, firstRowIndex);
            if (executor == null) {
                invokeOrdered(batch);
            } else {
                submit(batch);
            }
            return !reached && !stopped;
        }

        private List<T> truncate(List<T> rows) {
            if (options.maxRows() < 0) {
                return rows;
            }
            int remaining = options.maxRows() - totalRows.get();
            if (remaining <= 0) {
                truncated = true;
                return List.of();
            }
            if (rows.size() > remaining) {
                truncated = true;
                return rows.subList(0, remaining);
            }
            return rows;
        }

        private boolean reachedMaxRows() {
            return options.maxRows() >= 0 && totalRows.get() >= options.maxRows();
        }

        private void invokeOrdered(ReadBatch<T> batch) {
            try {
                invoke(batch);
            } catch (RuntimeException e) {
                handlerFailure.compareAndSet(null, e);
                throw e;
            }
        }

        private void submit(ReadBatch<T> batch) {
            try {
                permits.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BanyanException(ErrorCode.PARSE_FAILED, e);
            }
            try {
                executor.execute(() -> {
                    try {
                        invoke(batch);
                    } catch (RuntimeException e) {
                        workerFailure.compareAndSet(null, e);
                        stopped = true;
                    } finally {
                        permits.release();
                    }
                });
            } catch (RuntimeException e) {
                permits.release();
                throw e;
            }
        }

        private void invoke(ReadBatch<T> batch) {
            try {
                handler.handle(batch);
            } catch (RuntimeException e) {
                if (options.onError() == null) {
                    throw e;
                }
                RowError error = new RowError(batch.firstRowIndex(), e.getMessage(), e);
                addError(error);
                options.onError().accept(error);
            }
        }

        void addError(RowError error) {
            errors.add(error);
        }

        void awaitCompletion() {
            if (executor == null) {
                return;
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(AWAIT_TIMEOUT_HOURS, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                throw new BanyanException(ErrorCode.PARSE_FAILED, e);
            }
            RuntimeException failure = workerFailure.get();
            if (failure != null) {
                handlerFailure.compareAndSet(null, failure);
                throw failure;
            }
        }

        void abort() {
            stopped = true;
            if (executor != null) {
                executor.shutdownNow();
            }
        }

        void shutdown() {
            if (executor != null) {
                executor.shutdownNow();
            }
        }

        RuntimeException handlerFailure() {
            return handlerFailure.get();
        }

        int totalRows() {
            return totalRows.get();
        }

        int batchCount() {
            return batchCount.get();
        }

        boolean truncated() {
            return truncated;
        }

        List<RowError> errors() {
            return errors;
        }
    }
}
