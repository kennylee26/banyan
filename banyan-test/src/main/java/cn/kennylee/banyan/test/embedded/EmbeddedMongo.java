package cn.kennylee.banyan.test.embedded;

/**
 * 内嵌 MongoDB 的测试常量。flapdoodle 由 Spring Boot 自动配置拉起，使用方只需提供版本属性。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class EmbeddedMongo {

    /** 内嵌 Mongo 版本属性名。 */
    public static final String VERSION_PROPERTY = "de.flapdoodle.mongodb.embedded.version";

    /** 默认内嵌 Mongo 版本。 */
    public static final String DEFAULT_VERSION = "8.0.3";

    private EmbeddedMongo() {
    }
}
