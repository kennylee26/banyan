package cn.kennylee.banyan.excel.parser;

import java.time.Duration;
import java.util.List;

/**
 * 一次同步解析的结果描述。
 *
 * <p>只包含统计与错误，不包含全量数据；需要数据时使用批次处理器或全量读取方法。</p>
 *
 * @param totalRows  实际读取的行数
 * @param batchCount 批次数量
 * @param spentTime  耗时
 * @param truncated  是否因达到最大行数而截断
 * @param errors     行级错误，未启用错误收集时为空
 * @author kennylee
 * @since 0.1.0
 */
public record ParseResult(int totalRows, int batchCount, Duration spentTime,
                          boolean truncated, List<RowError> errors) {

    /**
     * 复制错误列表并兜底耗时。
     */
    public ParseResult {
        spentTime = spentTime == null ? Duration.ZERO : spentTime;
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
