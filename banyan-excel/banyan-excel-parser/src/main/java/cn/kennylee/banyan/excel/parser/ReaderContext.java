package cn.kennylee.banyan.excel.parser;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 读取器打开时的上下文：输入源、目标类型、格式、sheet、表头与批次等配置。
 *
 * <p>由公共层构造并传给实现模块；实现模块只读取这些配置，不修改。</p>
 *
 * @param source         输入源；由调用方或公共层管理关闭，读取器不得关闭
 * @param rowType        目标行类型
 * @param format         输入格式
 * @param sheet          选中的 sheet
 * @param headRowNumber  表头行数
 * @param batchSize      批次大小
 * @param ignoreEmptyRow 是否忽略空行
 * @param headerAliases  表头文案到字段名的别名映射
 * @param onConvertError 行转换错误回调；为 {@code null} 时实现模块应快速失败
 * @param <T>            行类型
 * @author kennylee
 * @since 0.1.0
 */
public record ReaderContext<T>(InputStream source, Class<T> rowType, ParseFormat format, SheetRef sheet,
                               int headRowNumber, int batchSize, boolean ignoreEmptyRow,
                               Map<String, String> headerAliases,
                               Consumer<RowError> onConvertError) {

    /**
     * 复制别名映射，避免外部修改。
     */
    public ReaderContext {
        headerAliases = headerAliases == null ? Map.of() : Map.copyOf(headerAliases);
    }
}
