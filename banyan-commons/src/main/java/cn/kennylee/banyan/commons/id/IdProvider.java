package cn.kennylee.banyan.commons.id;

/**
 * ID 生成器。按主键类型分别生成，使用方可替换实现（例如分布式 ID、规避雪花重复问题的自定义实现）。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface IdProvider {

    /**
     * 生成 String 类型主键。
     *
     * @return 字符串 ID
     */
    String nextId();

    /**
     * 生成 Long 类型主键。默认实现基于 JDK，仅保证单进程内不重复；分布式场景请替换实现。
     *
     * @return 正数 long ID
     */
    long nextLongId();
}
