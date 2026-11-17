package cn.kennylee.banyan.cache.autoconfigure;

import cn.kennylee.banyan.cache.support.CacheKeys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存配置。所有开关与参数集中在此，避免散读环境变量。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.cache")
public class CacheProperties {

    /** 装配总开关，默认 true；由自动装配条件读取。 */
    private boolean enabled = true;

    /** 读写开关，默认 true；关闭后所有读返回空、所有写 no-op。 */
    private boolean readWriteEnabled = true;

    /** key 最大长度，默认 512。 */
    private int maxKeyLength = CacheKeys.DEFAULT_MAX_KEY_LENGTH;

    /** 类型标记配置。 */
    private TypeMarker typeMarker = new TypeMarker();

    /** 穿透防护配置。 */
    private Penetration penetration = new Penetration();

    /** 锁配置。 */
    private Lock lock = new Lock();

    /** SCAN 批量删除配置。 */
    private Scan scan = new Scan();

    /**
     * 类型标记配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class TypeMarker {

        /**
         * 是否给本库写入的值附加类型标记，默认 true。
         *
         * <p>关闭后退回「信任调用方」模式：不再校验 `put`/`get` 与 `putRaw`/`getRaw`
         * 的格式，混用同一 key 会读到错位数据或解析失败，风险由调用方自行承担。</p>
         */
        private boolean enabled = true;
    }

    /**
     * 穿透防护配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Penetration {

        /** 是否启用穿透防护，默认关闭。 */
        private boolean enabled = false;
    }

    /**
     * 锁配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Lock {

        /** 删缓存锁的 TTL，默认 5 秒。 */
        private Duration ttl = Duration.ofSeconds(5);
    }

    /**
     * SCAN 批量删除配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Scan {

        /** 单批扫描与删除数量，默认 500。 */
        private int batchSize = 500;

        /** 单次调用最大删除数量，默认 1000。 */
        private int maxDeletions = 1000;
    }
}
