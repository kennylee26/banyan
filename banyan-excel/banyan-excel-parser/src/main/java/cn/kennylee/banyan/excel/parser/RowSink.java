package cn.kennylee.banyan.excel.parser;

import java.util.List;

/**
 * 批次接收端：实现模块把攒好的批次推给公共层。
 *
 * <p>由公共层的解析流程实现，使用方不直接实现。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
public interface RowSink<T> {

    /**
     * 接收一个批次。
     *
     * @param rows          批次内的行
     * @param firstRowIndex 第一行在文件中的物理行号
     * @return {@code true} 表示可以继续推送；{@code false} 表示已满足上限或已终止，请尽快停止
     */
    boolean accept(List<T> rows, int firstRowIndex);
}
