package cn.kennylee.banyan.data.mysql.id;

import cn.kennylee.banyan.commons.id.IdProvider;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;

/**
 * 委托 MyBatis-Plus {@link IdWorker} 的 ID 实现。
 * <p>{@code IdWorker.getId()} 默认经 {@code DefaultIdentifierGenerator} → {@code Sequence}（雪花算法）产出 long；
 * 但 {@code IdWorker} 持有全局可替换的 {@code IdentifierGenerator}，使用方调用
 * {@code initSequence} / {@code setIdentifierGenerator} 或配置 GlobalConfig 后，产出的不一定是雪花 ID。
 * 本类尊重该全局配置，不单独实现雪花。</p>
 * <p>已知风险：{@code Sequence} 依赖 workerId / datacenterId 正确分配，容器化环境 MAC 相同或时钟回拨时
 * 可能产出重复 ID。需要规避时，实现自己的 {@link IdProvider} 并通过 {@code BaseDas.setIdProvider} 替换。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MybatisIdWorkerProvider implements IdProvider {

    /**
     * 生成 String 类型主键。
     *
     * @return 雪花 ID 字符串
     */
    @Override
    public String nextId() {
        return IdWorker.getIdStr();
    }

    /**
     * 生成 Long 类型主键。
     *
     * @return 雪花 ID
     */
    @Override
    public long nextLongId() {
        return IdWorker.getId();
    }
}
