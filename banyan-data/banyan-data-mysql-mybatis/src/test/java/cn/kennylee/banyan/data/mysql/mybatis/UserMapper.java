package cn.kennylee.banyan.data.mysql.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试 Mapper。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEo> {
}
