package cn.kennylee.banyan.excel.parser;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 解析配置 Builder：批次大小、sheet、表头、上限、保序、并发与错误策略。
 *
 * <p>默认值面向安全：批次 500、保序开启、并发度 1、不限行数、行转换失败快速失败。全部配置都是调用参数，
 * 不从配置文件读取。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
public final class ExcelParserBuilder<T> {

    /** 默认批次大小。 */
    public static final int DEFAULT_BATCH_SIZE = 500;

    /** 表示不限制最大行数。 */
    public static final int UNLIMITED = -1;

    private final ExcelRowReader<T> reader;

    private final Class<T> rowType;

    private ParseFormat format = ParseFormat.XLSX;

    private SheetRef sheet = SheetRef.ofIndex(0);

    private int headRowNumber = 1;

    private int batchSize = DEFAULT_BATCH_SIZE;

    private int maxRows = UNLIMITED;

    private boolean ignoreEmptyRow = true;

    private final Map<String, String> headerAliases = new LinkedHashMap<>();

    private boolean ordered = true;

    private int parallelism = 1;

    private Integer maxPendingBatches;

    private Consumer<RowError> onError;

    private Consumer<RowError> onConvertError;

    private ExcelParserBuilder(ExcelRowReader<T> reader, Class<T> rowType) {
        this.reader = reader;
        this.rowType = rowType;
    }

    /**
     * 创建 Builder。
     *
     * @param reader  行读取器实现
     * @param rowType 目标行类型
     * @param <T>     行类型
     * @return Builder
     * @throws BanyanException 读取器或行类型为空
     */
    public static <T> ExcelParserBuilder<T> of(ExcelRowReader<T> reader, Class<T> rowType) {
        if (reader == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "reader");
        }
        if (rowType == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "rowType");
        }
        return new ExcelParserBuilder<>(reader, rowType);
    }

    /**
     * 设置输入格式。
     *
     * @param format 输入格式
     * @return 当前 Builder
     * @throws BanyanException 输入格式为空
     */
    public ExcelParserBuilder<T> format(ParseFormat format) {
        if (format == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "format");
        }
        this.format = format;
        return this;
    }

    /**
     * 设置要读取的 sheet；名称或索引二选一。
     *
     * @param sheet sheet 选择
     * @return 当前 Builder
     * @throws BanyanException sheet 选择为空
     */
    public ExcelParserBuilder<T> sheet(SheetRef sheet) {
        if (sheet == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "sheet");
        }
        this.sheet = sheet;
        return this;
    }

    /**
     * 设置表头行数。
     *
     * @param headRowNumber 表头行数
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> headRowNumber(int headRowNumber) {
        this.headRowNumber = headRowNumber;
        return this;
    }

    /**
     * 设置批次大小。
     *
     * @param batchSize 批次大小
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * 设置最大读取行数。
     *
     * @param maxRows 最大行数，{@link #UNLIMITED} 表示不限
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> maxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

    /**
     * 设置是否忽略空行。
     *
     * @param ignoreEmptyRow 是否忽略空行
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> ignoreEmptyRow(boolean ignoreEmptyRow) {
        this.ignoreEmptyRow = ignoreEmptyRow;
        return this;
    }

    /**
     * 添加一个表头别名。
     *
     * @param header 表头文案
     * @param field  字段名
     * @return 当前 Builder
     * @throws BanyanException 表头文案或字段名为空
     */
    public ExcelParserBuilder<T> headerAlias(String header, String field) {
        if (header == null || field == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "headerAlias");
        }
        this.headerAliases.put(header, field);
        return this;
    }

    /**
     * 批量添加表头别名。
     *
     * @param headerAliases 表头文案到字段名的映射
     * @return 当前 Builder
     * @throws BanyanException 映射为空
     */
    public ExcelParserBuilder<T> headerAliases(Map<String, String> headerAliases) {
        if (headerAliases == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "headerAliases");
        }
        this.headerAliases.putAll(headerAliases);
        return this;
    }

    /**
     * 设置是否保序。
     *
     * @param ordered 是否按文件行序串行回调
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> ordered(boolean ordered) {
        this.ordered = ordered;
        return this;
    }

    /**
     * 设置处理阶段并发度。
     *
     * @param parallelism 并发度，1 表示串行
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> parallelism(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    /**
     * 设置在途批次上限；默认等于并发度的两倍。
     *
     * @param maxPendingBatches 在途批次上限
     * @return 当前 Builder
     */
    public ExcelParserBuilder<T> maxPendingBatches(int maxPendingBatches) {
        this.maxPendingBatches = maxPendingBatches;
        return this;
    }

    /**
     * 设置批次处理异常回调；配置后异常会被记录并继续读取。
     *
     * @param onError 错误回调
     * @return 当前 Builder
     * @throws BanyanException 回调为空
     */
    public ExcelParserBuilder<T> onError(Consumer<RowError> onError) {
        if (onError == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "onError");
        }
        this.onError = onError;
        return this;
    }

    /**
     * 设置行转换错误回调；配置后失败行会被记录并跳过。
     *
     * @param onConvertError 转换错误回调
     * @return 当前 Builder
     * @throws BanyanException 回调为空
     */
    public ExcelParserBuilder<T> onConvertError(Consumer<RowError> onConvertError) {
        if (onConvertError == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "onConvertError");
        }
        this.onConvertError = onConvertError;
        return this;
    }

    /**
     * 读取输入流。
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
        return build().read(source, handler);
    }

    /**
     * 读取文件；文件流由库打开并关闭。
     *
     * @param source  文件
     * @param handler 批次处理器
     * @return 解析结果
     * @throws BanyanException 读取失败或配置非法
     */
    public ParseResult read(File source, BatchHandler<T> handler) {
        if (source == null) {
            throw new BanyanException(ErrorCode.PARSE_REQUIRED, "source");
        }
        try (InputStream input = Files.newInputStream(source.toPath())) {
            return build().read(input, handler);
        } catch (IOException e) {
            throw new BanyanException(ErrorCode.PARSE_FAILED, e);
        }
    }

    /**
     * 读取全部行；全量驻留内存，仅适合小文件。
     *
     * @param source 输入流
     * @return 全部行，顺序与文件一致
     * @throws BanyanException 读取失败或配置非法
     */
    public List<T> readAll(InputStream source) {
        List<T> rows = new ArrayList<>();
        read(source, batch -> rows.addAll(batch.rows()));
        return List.copyOf(rows);
    }

    /**
     * 读取文件中的全部行；全量驻留内存，仅适合小文件。
     *
     * @param source 文件
     * @return 全部行，顺序与文件一致
     * @throws BanyanException 读取失败或配置非法
     */
    public List<T> readAll(File source) {
        List<T> rows = new ArrayList<>();
        read(source, batch -> rows.addAll(batch.rows()));
        return List.copyOf(rows);
    }

    /**
     * 构建解析器。
     *
     * @return 解析器
     * @throws BanyanException 配置非法
     */
    public ExcelParser<T> build() {
        validate();
        int pending = maxPendingBatches == null ? parallelism * 2 : maxPendingBatches;
        ReadOptions<T> options = new ReadOptions<>(rowType, format, sheet, headRowNumber, batchSize,
            maxRows, ignoreEmptyRow, headerAliases, ordered, parallelism, Math.max(1, pending),
            onError, onConvertError);
        return new ExcelParser<>(reader, options);
    }

    private void validate() {
        if (batchSize <= 0) {
            throw configInvalid("batchSize");
        }
        if (headRowNumber < 1) {
            throw configInvalid("headRowNumber");
        }
        if (maxRows == 0 || maxRows < UNLIMITED) {
            throw configInvalid("maxRows");
        }
        if (parallelism <= 0) {
            throw configInvalid("parallelism");
        }
        if (maxPendingBatches != null && maxPendingBatches <= 0) {
            throw configInvalid("maxPendingBatches");
        }
    }

    private static BanyanException configInvalid(String field) {
        return new BanyanException(ErrorCode.PARSE_CONFIG_INVALID, field);
    }
}
