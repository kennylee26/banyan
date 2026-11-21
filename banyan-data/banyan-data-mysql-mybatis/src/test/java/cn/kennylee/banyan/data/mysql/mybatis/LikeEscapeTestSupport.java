package cn.kennylee.banyan.data.mysql.mybatis;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * LIKE 转义测试的共用种子数据与查询构造。
 *
 * @author kennylee
 * @since 0.1.0
 */
abstract class LikeEscapeTestSupport {

    /** 测试数据名称前缀。 */
    protected static final String NAME_PREFIX = "pg-";

    @Autowired
    protected UserDas userDas;

    @BeforeEach
    void seedLikeEscapeData() {
        userDas.saveBatch(List.of(
                newUser("pg-50%off"),
                newUser("pg-50xoff"),
                newUser("pg-51_a"),
                newUser("pg-51xa")));
    }

    /**
     * 构造带前缀限定的模糊查询。
     *
     * @param likeValue 全模糊值
     * @return 查询入参
     */
    protected EscapeQuery query(String likeValue) {
        EscapeQuery query = new EscapeQuery();
        query.setUserNamePreLike(NAME_PREFIX);
        query.setUserNameLike(likeValue);
        query.setPageSize(100);
        return query;
    }

    /**
     * 构造测试实体。
     *
     * @param userName 用户名
     * @return 实体
     */
    protected UserEo newUser(String userName) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(20);
        return user;
    }
}
