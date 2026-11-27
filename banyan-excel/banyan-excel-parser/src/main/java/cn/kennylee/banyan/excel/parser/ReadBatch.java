package cn.kennylee.banyan.excel.parser;

import java.util.List;

/**
 * 一个读取批次：一批已解码的行，以及该批第一行在文件中的物理行号。
 *
 * <p>行号从 1 开始并计入表头行，用于错误定位与无序模式下的排序。批次内的行按文件顺序排列且不可修改；
 * 批次内各行的物理行号可能因跳过空行或转换失败行而不连续，需要逐行定位时以错误信息或实现模块提供的行号为准，
 * 排序时可按批次的 {@code firstRowIndex} 对批次排序再展开。</p>
 *
 * @param rows          批次内的行，按文件顺序排列
 * @param firstRowIndex 第一行在文件中的物理行号
 * @param <T>           行类型
 * @author kennylee
 * @since 0.1.0
 */
public record ReadBatch<T>(List<T> rows, int firstRowIndex) {

    /**
     * 复制批次行，保证调用方无法修改。
     */
    public ReadBatch {
        rows = List.copyOf(rows);
    }

    /**
     * 批次行数。
     *
     * @return 行数
     */
    public int size() {
        return rows.size();
    }
}
