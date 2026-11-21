package cn.kennylee.banyan.data.mongodb.springdata;

import cn.kennylee.banyan.data.mongodb.springdata.das.MongoDbEo;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 测试实体。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Data
@Document(collection = "users")
public class UserEo implements MongoDbEo<ObjectId> {

    /** 主键，默认 ObjectId。 */
    @Id
    private ObjectId id;

    /** 用户名。 */
    private String userName;

    /** 年龄。 */
    private Integer age;

    /** 邮箱。 */
    private String email;

    /** 城市。 */
    private String city;
}
