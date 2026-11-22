package cn.kennylee.banyan.mq.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 消息模型与默认编解码行为测试；编解码多实现契约见 commons-spring 的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqMessageTest {

    @Test
    void keepsAllFields() {
        // given
        Map<String, String> headers = new HashMap<>();
        headers.put("tenant", "t1");

        // when
        MqMessage message = MqMessage.builder()
                .body("{\"id\":1}")
                .headers(headers)
                .key("order-1")
                .timestamp(1700000000000L)
                .build();

        // then
        assertEquals("{\"id\":1}", message.getBody());
        assertEquals("t1", message.getHeaders().get("tenant"));
        assertEquals("order-1", message.getKey());
        assertEquals(1700000000000L, message.getTimestamp());
    }

    @Test
    void copiesHeadersDefensively() {
        // given
        Map<String, String> headers = new HashMap<>();
        headers.put("tenant", "t1");
        MqMessage message = MqMessage.builder().body("x").headers(headers).build();

        // when
        headers.put("tenant", "t2");

        // then
        assertEquals("t1", message.getHeaders().get("tenant"));
        assertThrows(UnsupportedOperationException.class,
                () -> message.getHeaders().put("other", "v"));
    }

    @Test
    void defaultCodecPassesStringThrough() {
        // given
        MqMessageCodec codec = new StringMqMessageCodec();

        // when
        String encoded = codec.encode("hello");
        Object decoded = codec.decode("hello", String.class);

        // then
        assertEquals("hello", encoded);
        assertEquals("hello", decoded);
    }

    @Test
    void defaultCodecRejectsUnsupportedType() {
        // given
        MqMessageCodec codec = new StringMqMessageCodec();

        // when
        assertThrows(IllegalArgumentException.class, () -> codec.decode("x", Integer.class));
    }

    @Test
    void convertsDataToBeanWithBoundCodec() {
        // given
        MqMessageCodec codec = new MqMessageCodec() {
            @Override
            public String encode(Object payload) {
                return String.valueOf(payload);
            }

            @Override
            public Object decode(String body, Class<?> type) {
                return new Foo(body);
            }
        };
        MqMessage message = MqMessage.builder().body("x").codec(codec).build();

        // when
        Foo foo = message.getData().toBean(Foo.class);

        // then
        assertEquals("x", foo.value);
    }

    @Test
    void rejectsDataConversionWithoutCodec() {
        // given
        MqMessage message = MqMessage.builder().body("x").build();

        // when
        assertThrows(MqConfigurationException.class,
                () -> message.getData().toBean(String.class));
    }

    @Test
    void convertsGenericDataWithType() {
        // given
        MqMessageCodec codec = new MqMessageCodec() {
            @Override
            public String encode(Object payload) {
                return String.valueOf(payload);
            }

            @Override
            public Object decode(String body, Class<?> type) {
                return body;
            }

            @Override
            public Object decode(String body, java.lang.reflect.Type type) {
                return java.util.List.of("a", "b");
            }
        };
        MqMessage message = MqMessage.builder().body("[\"a\",\"b\"]").codec(codec).build();

        // when
        java.lang.reflect.Type listOfString = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type getRawType() {
                return java.util.List.class;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }

            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[]{String.class};
            }
        };
        java.util.List<String> values = message.getData().toBean(listOfString);

        // then
        assertEquals(java.util.List.of("a", "b"), values);
    }

    /**
     * 测试用业务对象。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class Foo {

        private final String value;

        Foo(String value) {
            this.value = value;
        }
    }
}
