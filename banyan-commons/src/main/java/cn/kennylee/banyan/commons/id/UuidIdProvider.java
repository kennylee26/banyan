package cn.kennylee.banyan.commons.id;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 JDK 的默认实现：String 用 UUID 去横线，Long 用进程内自增。
 * <p>不实现雪花算法，分布式或有严格唯一性要求的场景请替换为其他实现。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class UuidIdProvider implements IdProvider {

    /** 进程内自增序列，用于 Long 主键。 */
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() & Long.MAX_VALUE);

    /**
     * 生成 String 类型主键。
     *
     * @return UUID 去横线后的 32 位字符串
     */
    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成 Long 类型主键。
     *
     * @return 正数 long ID
     */
    @Override
    public long nextLongId() {
        return sequence.incrementAndGet();
    }
}
