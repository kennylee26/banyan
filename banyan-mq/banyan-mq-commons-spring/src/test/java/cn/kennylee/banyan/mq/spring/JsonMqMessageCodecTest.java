package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.commons.spring.json.JsonCodec;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JSON 编解码的契约测试：用纯 JDK 假 JsonCodec 验证委托与 String 直通，不引入 Jackson。
 *
 * @author kennylee
 * @since 0.1.0
 */
class JsonMqMessageCodecTest extends MqMessageCodecContractTest {

    private final MqMessageCodec codec = new JsonMqMessageCodec(new FakeJsonCodec());

    @Override
    protected MqMessageCodec codec() {
        return codec;
    }

    @Test
    void delegatesPojoEncodingToJsonCodec() {
        // when
        String encoded = codec.encode(new Foo("order-1", 2));

        // then
        assertEquals("order-1:2", encoded);
    }

    @Test
    void delegatesPojoDecodingToJsonCodec() {
        // when
        Foo decoded = (Foo) codec.decode("order-1:2", Foo.class);

        // then
        assertEquals(new Foo("order-1", 2), decoded);
    }

    @Test
    void delegatesGenericDecodingToJsonCodec() {
        // when
        @SuppressWarnings("unchecked")
        List<String> decoded = (List<String>) codec.decode("[\"a\",\"b\"]", (Type) List.class);

        // then
        assertEquals(List.of("a", "b"), decoded);
    }

    /**
     * 测试用业务对象。
     *
     * @param name  名称
     * @param count 数量
     * @author kennylee
     * @since 0.1.0
     */
    record Foo(String name, int count) {
    }

    /**
     * 纯 JDK 假 JsonCodec：只用「name:count」格式，避免测试引入 Jackson。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class FakeJsonCodec implements JsonCodec {

        @Override
        public String toJson(Object value) {
            Foo foo = (Foo) value;
            return foo.name() + ":" + foo.count();
        }

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            String[] parts = json.split(":");
            @SuppressWarnings("unchecked")
            T value = (T) new Foo(parts[0], Integer.parseInt(parts[1]));
            return value;
        }

        @Override
        public Object fromJson(String json, Type type) {
            return List.of("a", "b");
        }
    }
}
