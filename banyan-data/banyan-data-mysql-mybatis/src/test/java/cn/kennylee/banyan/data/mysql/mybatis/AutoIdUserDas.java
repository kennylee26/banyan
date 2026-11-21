package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mysql.mybatis.das.BaseDas;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 自增主键测试数据访问类：不继承 BaseEo，演示使用方自定义主键策略。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class AutoIdUserDas extends BaseDas<AutoIdUserEo, Long> {

    private final AutoIdUserMapper mapper;

    public AutoIdUserDas(AutoIdUserMapper mapper, DasQueryStrategyRegistry registry) {
        super(registry);
        this.mapper = mapper;
    }

    @Override
    public BaseMapper<AutoIdUserEo> getBaseMapper() {
        return mapper;
    }

    @Override
    public Class<AutoIdUserEo> getEntityClass() {
        return AutoIdUserEo.class;
    }

    @Override
    public Class<? extends BaseMapper<AutoIdUserEo>> getMapperClass() {
        return AutoIdUserMapper.class;
    }
}
