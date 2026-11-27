package cn.kennylee.banyan.excel.parser;

/**
 * 批次处理器：使用方实现，每次接收一个读取批次。
 *
 * <p>处理器决定批次数据是留存、存库还是丢弃，库不提供结果汇总通道。处理器抛出的运行时异常默认终止读取，
 * 可通过 {@link ExcelParserBuilder#onError(java.util.function.Consumer)} 改为记录后继续。</p>
 *
 * @param <T> 行类型
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface BatchHandler<T> {

    /**
     * 处理一个批次。
     *
     * @param batch 读取批次
     */
    void handle(ReadBatch<T> batch);
}
