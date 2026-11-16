package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mysql.strategy.MyBatisDasQueryStrategy;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.mybatis.spring.annotation.MapperScan;

import java.util.Map;

/**
 * 测试应用配置。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootApplication
@MapperScan("cn.kennylee.banyan.data.mysql")
public class TestApp {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }

    @Bean
    public DasQueryStrategyRegistry dasQueryStrategyRegistry() {
        return new DefaultDasQueryStrategyRegistry(
                Map.of(DasDbType.MYSQL, new MyBatisDasQueryStrategy<>(UserEo.class)));
    }

    @Bean
    public UserDas userDas(UserMapper userMapper, DasQueryStrategyRegistry registry) {
        return new UserDas(userMapper, registry);
    }

    @Bean
    public AutoIdUserDas autoIdUserDas(AutoIdUserMapper mapper, DasQueryStrategyRegistry registry) {
        return new AutoIdUserDas(mapper, registry);
    }
}
