package cn.kennylee.banyan.data.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自增主键测试 Mapper。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Mapper
public interface AutoIdUserMapper extends BaseMapper<AutoIdUserEo> {
}
