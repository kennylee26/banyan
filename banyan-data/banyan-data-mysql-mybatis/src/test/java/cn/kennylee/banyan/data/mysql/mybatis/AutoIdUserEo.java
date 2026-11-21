package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.mysql.mybatis.das.BaseEo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试实体：在子类字段上声明 @TableId(AUTO)，演示使用方把默认雪花改为数据库自增。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class AutoIdUserEo extends BaseEo<Long> {

    /** 在子类字段上声明 AUTO，覆盖默认的全局雪花策略。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名。 */
    private String userName;

    /** 年龄。 */
    private Integer age;
}
