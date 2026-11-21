package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.mybatis.spring.annotation.MapperScan;


/**
 * 测试应用配置。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootApplication
@MapperScan("cn.kennylee.banyan.data.mysql.mybatis")
public class TestApp {

    @Bean
    public UserDas userDas(UserMapper userMapper, DasQueryStrategyRegistry registry) {
        return new UserDas(userMapper, registry);
    }

    @Bean
    public AutoIdUserDas autoIdUserDas(AutoIdUserMapper mapper, DasQueryStrategyRegistry registry) {
        return new AutoIdUserDas(mapper, registry);
    }
}
