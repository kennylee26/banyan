package cn.kennylee.banyan.commons.crypto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 可手动快进的测试时钟。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class MutableClock extends Clock {

    /** 当前时间；volatile 保证并发读到的推进结果可见。 */
    private volatile Instant instant;

    /**
     * 构造实例。
     *
     * @param instant 起始时间
     */
    MutableClock(Instant instant) {
        this.instant = instant;
    }

    /**
     * 快进。
     *
     * @param duration 时长
     */
    void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
