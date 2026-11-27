package cn.kennylee.banyan.excel.parser;

/**
 * 行级错误：文件行号、说明与可选的原始异常。
 *
 * <p>行号语义与 {@link ReadBatch#firstRowIndex()} 一致，从 1 开始并计入表头行。</p>
 *
 * @param rowIndex 出错行在文件中的物理行号
 * @param message  错误说明
 * @param cause    原始异常，可为 {@code null}
 * @author kennylee
 * @since 0.1.0
 */
public record RowError(int rowIndex, String message, Throwable cause) {
}
