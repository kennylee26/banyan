package cn.kennylee.banyan.data.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认（显式 false）不启用全表写防护：既有写操作语义不变。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class)
@Transactional
class BlockAttackDisabledIntegrationTest {

    @Autowired
    private WriteProbeMapper writeProbeMapper;

    /** 默认关闭时无 WHERE 的 UPDATE 能执行。 */
    @Test
    void allowsFullTableUpdateByDefault() {
        // when
        assertThat(writeProbeMapper.bumpAllAges()).isEqualTo(2);
    }
}
