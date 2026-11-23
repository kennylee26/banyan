package cn.kennylee.banyan.openapi.sign.support;

import cn.kennylee.banyan.commons.crypto.SignKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 配置驱动密钥解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignKeyResolverTest {

    /** 已知 keyId 返回对应密钥。 */
    @Test
    void resolvesConfiguredKey() {
        // given
        SignKeyResolver resolver = new ConfiguredSignKeyResolver(Map.of("app1", "secret"));

        // when
        SignKey key = resolver.resolve("app1");

        // then
        assertEquals(new SignKey("app1", "secret"), key);
    }

    /** 未知或空 keyId 返回 null。 */
    @Test
    void returnsNullForUnknownKey() {
        // given
        SignKeyResolver resolver = new ConfiguredSignKeyResolver(Map.of("app1", "secret"));

        // when
        assertNull(resolver.resolve("app2"));
        assertNull(resolver.resolve(null));
        assertNull(resolver.resolve(" "));
    }

    /** 配置的 secret 为空时启动失败。 */
    @Test
    void rejectsBlankSecretAtStartup() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguredSignKeyResolver(Map.of("app1", " ")));
    }

    /** 配置的 keyId 为空时启动失败。 */
    @Test
    void rejectsBlankKeyIdAtStartup() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguredSignKeyResolver(Map.of(" ", "secret")));
    }

    /** 构造后修改原 Map 不影响解析结果。 */
    @Test
    void copiesConfiguration() {
        // given
        Map<String, String> source = new HashMap<>();
        source.put("app1", "secret");
        SignKeyResolver resolver = new ConfiguredSignKeyResolver(source);
        source.put("app1", "changed");

        // when
        SignKey key = resolver.resolve("app1");

        // then
        assertEquals(new SignKey("app1", "secret"), key);
    }
}
