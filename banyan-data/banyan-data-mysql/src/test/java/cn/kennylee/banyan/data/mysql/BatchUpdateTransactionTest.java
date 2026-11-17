package cn.kennylee.banyan.data.mysql;

import cn.kennylee.banyan.commons.error.BanyanException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批量更新的事务骨架测试：非事务模式下任一分片失败要整体回滚。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
class BatchUpdateTransactionTest {

    @Autowired
    private UserDas userDas;

    private final List<Long> createdIds = new ArrayList<>();

    /** 非事务模式：第一片成功、第二片失败时，已执行分片必须回滚。 */
    @Test
    void rollsBackAllChunksWhenOneChunkFails() {
        UserEo first = newUser("zed");
        UserEo second = newUser("abe");
        userDas.save(first);
        userDas.save(second);
        createdIds.add(first.getId());
        createdIds.add(second.getId());
        first.setAge(88);
        // 超过列宽，第二个分片执行时报错
        second.setNickName("x".repeat(100));

        assertThatThrownBy(() -> userDas.updateBatchById(List.of(first, second), 1))
                .isInstanceOf(BanyanException.class);
        // 第一片已执行，但整体回滚后必须回到更新前的值
        assertThat(userDas.findById(first.getId()).getAge()).isEqualTo(20);
    }

    /** 非事务模式：全部分片成功后统一提交。 */
    @Test
    void commitsAllChunksWhenAllSucceed() {
        UserEo first = newUser("bob2");
        UserEo second = newUser("cat2");
        userDas.save(first);
        userDas.save(second);
        createdIds.add(first.getId());
        createdIds.add(second.getId());
        first.setAge(81);
        second.setAge(82);

        assertThat(userDas.updateBatchById(List.of(first, second), 1)).isEqualTo(2);
        assertThat(userDas.findById(first.getId()).getAge()).isEqualTo(81);
        assertThat(userDas.findById(second.getId()).getAge()).isEqualTo(82);
    }

    @AfterEach
    void cleanUp() {
        if (!createdIds.isEmpty()) {
            userDas.removeBatchByIds(createdIds);
            createdIds.clear();
        }
    }

    private UserEo newUser(String userName) {
        UserEo user = new UserEo();
        user.setUserName(userName);
        user.setAge(20);
        return user;
    }
}
