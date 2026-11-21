package cn.kennylee.banyan.data.mysql.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 验证「自定义 XML 查询首参传 Page 即分页」的测试 Mapper。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Mapper
public interface PagingProbeMapper {

    /**
     * 按最小年龄查询，SQL 中不写任何分页语法。
     *
     * @param page   分页参数
     * @param minAge 最小年龄
     * @return 分页结果
     */
    IPage<UserEo> selectByMinAge(Page<UserEo> page, @Param("minAge") Integer minAge);

    /**
     * 手写 XML 自己拼接首尾通配符的模糊查询。
     *
     * @param keyword 关键字
     * @return 实体列表
     */
    java.util.List<UserEo> selectByNameLike(@Param("keyword") String keyword);
}
