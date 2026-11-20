package cn.kennylee.banyan.data.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批量更新列数上限配置的端到端测试：验证配置项真正生效。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.max-update-columns=2")
@Transactional
class BatchUpdatePropertyTest {

    @Autowired
    private UserDas userDas;

    /** 配置为 2 时，3 列字段并集整批降级到框架批量。 */
    @Test
    void configuredLimitTakesEffect() {
        // given
        UserEo user = new UserEo();
        user.setUserName("yuri");
        user.setAge(75);
        user.setCity("hangzhou");
        userDas.save(user);
        user.setAge(76);
        user.setCity("foshan");

        // when
        assertThat(userDas.updateBatchById(List.of(user))).isEqualTo(-1);

        // then
        assertThat(userDas.findById(user.getId()).getAge()).isEqualTo(76);
    }
}
