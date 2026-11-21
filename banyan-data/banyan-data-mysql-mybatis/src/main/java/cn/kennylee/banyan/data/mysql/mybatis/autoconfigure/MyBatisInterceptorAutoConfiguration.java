package cn.kennylee.banyan.data.mysql.mybatis.autoconfigure;

import cn.kennylee.banyan.data.mysql.mybatis.support.LikeEscapeInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Plus 插件容器自动配置：分页默认启用，全表写防护默认显式关闭，使用方自定义的
 * {@link InnerInterceptor} bean 追加在库自带插件之后。
 * <p>使用者自行提供 {@link MybatisPlusInterceptor} 时整体让位，不补装、不合并，需要完全控制
 * 顺序时请自行创建容器 bean。</p>
 * <p><b>全表写防护边界：</b>只拦截缺少有效 WHERE 条件的 {@code UPDATE} / {@code DELETE}，
 * 防止全表更新与全表删除；<b>不防护 SQL 注入</b>，参数拼接类攻击需由使用方自行处理。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor")
@ConditionalOnProperty(prefix = "banyan.data-mysql", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MyBatisDasProperties.class)
public class MyBatisInterceptorAutoConfiguration {

    /**
     * 创建插件容器。
     *
     * @param properties         配置
     * @param customInterceptors 使用方声明的内层插件
     * @return 插件容器
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor banyanMybatisPlusInterceptor(
            MyBatisDasProperties properties, ObjectProvider<InnerInterceptor> customInterceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 顺序固定：LIKE 转义 → 分页 → 阻断；转义必须在分页之前
        if (properties.getLikeEscape().getInnerInterceptor().isEnabled()) {
            // 策略层启用时，wrapper 参数已由策略层转义，全局层只处理手写 SQL 的顶层参数
            interceptor.addInnerInterceptor(
                    new LikeEscapeInnerInterceptor(properties.getLikeEscape().isEnabled()));
        }
        if (properties.getPagination().isEnabled()) {
            interceptor.addInnerInterceptor(paginationInterceptor(properties.getPagination()));
        }
        if (properties.getBlockAttack().isEnabled()) {
            interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        }
        customInterceptors.orderedStream().forEach(interceptor::addInnerInterceptor);
        return interceptor;
    }

    /**
     * 按配置构造分页插件。
     *
     * @param pagination 分页配置
     * @return 分页插件
     */
    private PaginationInnerInterceptor paginationInterceptor(MyBatisDasProperties.Pagination pagination) {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor();
        interceptor.setMaxLimit(pagination.getMaxLimit());
        interceptor.setOverflow(pagination.isOverflow());
        if (StringUtils.hasText(pagination.getDbType())) {
            interceptor.setDbType(DbType.getDbType(pagination.getDbType().trim()));
        }
        return interceptor;
    }
}
