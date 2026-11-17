package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.data.mysql.das.BaseEo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 测试实体。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class UserEo extends BaseEo<Long> {

    /** 主键。无 @TableId，按全局 ASSIGN_ID 分配雪花 ID。 */
    private Long id;

    /** 用户名。 */
    @TableField("user_name")
    private String userName;

    /** 年龄。 */
    private Integer age;

    /** 邮箱。 */
    private String email;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 昵称。 */
    private String nickName;

    /** 城市。 */
    private String city;

    /** 状态。 */
    private Integer status;
}
