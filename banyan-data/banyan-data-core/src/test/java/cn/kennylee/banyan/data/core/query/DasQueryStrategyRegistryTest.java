package cn.kennylee.banyan.data.core.query;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询策略注册表测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class DasQueryStrategyRegistryTest {

    /** 按类型注册与获取策略。 */
    @Test
    void registersAndResolvesByType() {
        // given
        FakeStrategy mysql = new FakeStrategy();
        DasQueryStrategyRegistry registry = new DefaultDasQueryStrategyRegistry(Map.of(DasDbType.MYSQL, mysql));

        // when
        assertSame(mysql, registry.getStrategy(DasDbType.MYSQL));
        assertTrue(registry.containsStrategy(DasDbType.MYSQL));
        assertFalse(registry.containsStrategy(DasDbType.MONGODB));
    }

    /** 未注册类型返回 null。 */
    @Test
    void unregisteredTypeResolvesToNull() {
        // given
        DasQueryStrategyRegistry registry = new DefaultDasQueryStrategyRegistry(Map.of());

        // when
        assertNull(registry.getStrategy(DasDbType.MONGODB));
        assertFalse(registry.containsStrategy(DasDbType.MONGODB));
    }

    /** 运行期注册与移除。 */
    @Test
    void canRegisterAndRemoveAtRuntime() {
        // given
        FakeStrategy strategy = new FakeStrategy();
        DasQueryStrategyRegistry registry = new DefaultDasQueryStrategyRegistry(Map.of());

        // when
        registry.register(DasDbType.MONGODB, strategy);
        assertSame(strategy, registry.getStrategy(DasDbType.MONGODB));

        registry.remove(DasDbType.MONGODB);
        assertFalse(registry.containsStrategy(DasDbType.MONGODB));
    }

    /** 不存在静态单例入口。 */
    @Test
    void hasNoStaticSingletonAccessor() {
        // when
        assertThrows(NoSuchMethodException.class,
                () -> DasQueryStrategyRegistry.class.getMethod("getInstance"));
    }

    static class FakeStrategy implements DasQueryStrategy<Object, Object> {
        @Override
        public Object parseQuery(Object searchExample, Class<Object> entityClass,
                                 Collection<String> ignoredProperties) {
            return null;
        }

        @Override
        public DasQueryStrategy<Object, Object> newInstance() {
            return this;
        }
    }
}
