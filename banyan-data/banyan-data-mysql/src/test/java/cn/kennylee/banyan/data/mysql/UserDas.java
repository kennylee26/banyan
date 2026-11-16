package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mysql.das.BaseDas;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 测试数据访问类。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class UserDas extends BaseDas<UserEo, Long> {

    private final UserMapper userMapper;

    public UserDas(UserMapper userMapper, DasQueryStrategyRegistry registry) {
        super(registry);
        this.userMapper = userMapper;
    }

    @Override
    public BaseMapper<UserEo> getBaseMapper() {
        return userMapper;
    }

    @Override
    public Class<UserEo> getEntityClass() {
        return UserEo.class;
    }

    @Override
    public Class<? extends BaseMapper<UserEo>> getMapperClass() {
        return UserMapper.class;
    }
}
