package cn.kennylee.banyan.excel.parser;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 解析流程的不可变配置，由 Builder 校验后组装。
 *
 * @param rowType           目标行类型
 * @param format            输入格式
 * @param sheet             选中的 sheet
 * @param headRowNumber     表头行数
 * @param batchSize         批次大小
 * @param maxRows           最大读取行数，{@code -1} 表示不限
 * @param ignoreEmptyRow    是否忽略空行
 * @param headerAliases     表头文案到字段名的别名映射
 * @param ordered           是否保序
 * @param parallelism       处理阶段并发度
 * @param maxPendingBatches 在途批次上限
 * @param onError           批次处理异常回调，为 {@code null} 时快速失败
 * @param onConvertError    行转换错误回调，为 {@code null} 时快速失败
 * @param <T>               行类型
 * @author kennylee
 * @since 0.1.0
 */
record ReadOptions<T>(Class<T> rowType, ParseFormat format, SheetRef sheet, int headRowNumber,
                      int batchSize, int maxRows, boolean ignoreEmptyRow,
                      Map<String, String> headerAliases, boolean ordered, int parallelism,
                      int maxPendingBatches, Consumer<RowError> onError,
                      Consumer<RowError> onConvertError) {

    /**
     * 复制别名映射，避免外部修改。
     */
    ReadOptions {
        headerAliases = headerAliases == null ? Map.of() : Map.copyOf(headerAliases);
    }
}
