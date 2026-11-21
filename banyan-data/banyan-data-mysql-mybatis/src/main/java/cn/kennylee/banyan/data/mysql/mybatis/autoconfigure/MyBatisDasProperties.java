package cn.kennylee.banyan.data.mysql.mybatis.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MySQL 数据访问配置。所有开关与参数集中在此，避免散读环境变量。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.data-mysql")
public class MyBatisDasProperties {

    /** 分页配置。 */
    private Pagination pagination = new Pagination();

    /** LIKE 转义配置。 */
    private LikeEscape likeEscape = new LikeEscape();

    /** 全表更新/删除防护配置。 */
    private BlockAttack blockAttack = new BlockAttack();

    /**
     * 分页配置。
     */
    @Getter
    @Setter
    public static class Pagination {

        /** 是否启用分页，默认启用。 */
        private boolean enabled = true;

        /** 单页上限，-1 表示不限制。 */
        private long maxLimit = -1L;

        /** 页码超出末页时是否回到首页。 */
        private boolean overflow;

        /** 数据库类型，留空时由 MyBatis-Plus 自动探测。 */
        private String dbType;
    }

    /**
     * LIKE 转义配置。
     */
    @Getter
    @Setter
    public static class LikeEscape {

        /** 框架自身查询 DTO 产出的 like 条件是否转义，默认启用。 */
        private boolean enabled = true;

        /** 全局 SQL 层配置。 */
        private GlobalLayer innerInterceptor = new GlobalLayer();
    }

    /**
     * 全局 SQL 层 LIKE 转义配置。
     */
    @Getter
    @Setter
    public static class GlobalLayer {

        /** 是否启用全局 SQL 层转义，默认显式关闭。 */
        private boolean enabled = false;
    }

    /**
     * 全表更新/删除防护配置。
     */
    @Getter
    @Setter
    public static class BlockAttack {

        /** 是否启用全表更新/删除防护，默认显式关闭。 */
        private boolean enabled = false;
    }
}
