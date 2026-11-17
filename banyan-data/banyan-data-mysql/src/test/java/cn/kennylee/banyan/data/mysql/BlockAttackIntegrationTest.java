package cn.kennylee.banyan.data.mysql;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import org.mybatis.spring.MyBatisSystemException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 开启全表写防护后的行为测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.block-attack.enabled=true")
@Transactional
class BlockAttackIntegrationTest {

    @Autowired
    private WriteProbeMapper writeProbeMapper;

    @Autowired
    private UserDas userDas;

    /** 无 WHERE 的 UPDATE 被拦截，数据不变。 */
    @Test
    void blocksUpdateWithoutWhere() {
        assertThatThrownBy(() -> writeProbeMapper.bumpAllAges())
                .isInstanceOf(MyBatisSystemException.class)
                .hasRootCauseInstanceOf(MybatisPlusException.class)
                .hasRootCauseMessage("Prohibition of table update operation");

        assertThat(userDas.findAll()).extracting(UserEo::getAge)
                .containsExactlyInAnyOrder(20, 30);
    }

    /** 无 WHERE 的 DELETE 被拦截，数据不变。 */
    @Test
    void blocksDeleteWithoutWhere() {
        assertThatThrownBy(() -> writeProbeMapper.deleteAll())
                .isInstanceOf(MyBatisSystemException.class)
                .hasRootCauseInstanceOf(MybatisPlusException.class)
                .hasRootCauseMessage("Prohibition of full table deletion");

        assertThat(userDas.count()).isEqualTo(2);
    }

    /** 带 WHERE 的写操作正常执行。 */
    @Test
    void allowsWriteWithWhere() {
        assertThat(writeProbeMapper.bumpAgesFrom(25)).isEqualTo(1);
        assertThat(writeProbeMapper.deleteFrom(25)).isEqualTo(1);
    }
}
