package cn.kennylee.banyan.data.core.das;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAS 公共契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class DasOperationsContractTest {

    /** 批量按主键更新属于公共契约，返回数据库报告的影响行数。 */
    @Test
    void declaresBatchUpdateReturningInt() throws NoSuchMethodException {
        Method method = DasOperations.class.getMethod("updateBatchById", Collection.class);

        assertThat(method.getReturnType()).isEqualTo(int.class);
    }
}
