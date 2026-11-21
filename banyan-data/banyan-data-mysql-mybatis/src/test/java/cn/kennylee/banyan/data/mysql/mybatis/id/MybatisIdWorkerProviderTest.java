package cn.kennylee.banyan.data.mysql.mybatis.id;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis-Plus 雪花 ID 实现测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MybatisIdWorkerProviderTest {

    /** String 与 Long 两种形态都返回有效 ID。 */
    @Test
    void generatesBothIdTypes() {
        // given
        MybatisIdWorkerProvider provider = new MybatisIdWorkerProvider();

        // when
        String stringId = provider.nextId();
        long longId = provider.nextLongId();

        // then
        assertThat(stringId).isNotBlank();
        assertThat(Long.parseLong(stringId)).isPositive();
        assertThat(longId).isPositive();
    }
}
