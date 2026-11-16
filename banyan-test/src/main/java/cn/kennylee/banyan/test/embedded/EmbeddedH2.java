package cn.kennylee.banyan.test.embedded;

/**
 * 内嵌 H2 的测试常量与 JDBC URL 构造。使用方在 test 资源里放 schema.sql / data.sql 即可。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class EmbeddedH2 {

    /** H2 驱动类名。 */
    public static final String DRIVER = "org.h2.Driver";

    /** H2 用户名。 */
    public static final String USERNAME = "sa";

    /** H2 密码。 */
    public static final String PASSWORD = "";

    private EmbeddedH2() {
    }

    /**
     * 构造 H2 内存库地址。
     *
     * @param database 数据库名
     * @return H2 内存库地址
     */
    public static String memoryUrl(String database) {
        return "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1";
    }

    /**
     * 构造带初始化脚本的 H2 内存库地址。
     *
     * @param database     数据库名
     * @param schemaScript schema 脚本路径
     * @param dataScript   初始数据脚本路径
     * @return H2 内存库地址
     */
    public static String memoryUrlWithScripts(String database, String schemaScript, String dataScript) {
        return memoryUrl(database)
                + ";INIT=RUNSCRIPT FROM 'classpath:" + schemaScript + "'\\;RUNSCRIPT FROM 'classpath:" + dataScript + "'";
    }
}
