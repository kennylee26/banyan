package cn.kennylee.banyan.data.mysql.mybatis;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 全表写防护测试用的写操作 Mapper。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Mapper
public interface WriteProbeMapper {

    /**
     * 无 WHERE 的全表更新。
     *
     * @return 影响行数
     */
    @Update("UPDATE users SET age = age + 1")
    int bumpAllAges();

    /**
     * 带 WHERE 的更新。
     *
     * @param minAge 最小年龄
     * @return 影响行数
     */
    @Update("UPDATE users SET age = age + 1 WHERE age >= #{minAge}")
    int bumpAgesFrom(@Param("minAge") Integer minAge);

    /**
     * 无 WHERE 的全表删除。
     *
     * @return 影响行数
     */
    @Delete("DELETE FROM users")
    int deleteAll();

    /**
     * 带 WHERE 的删除。
     *
     * @param minAge 最小年龄
     * @return 影响行数
     */
    @Delete("DELETE FROM users WHERE age >= #{minAge}")
    int deleteFrom(@Param("minAge") Integer minAge);
}
