package cn.kennylee.banyan.commons.spring.json;

import cn.kennylee.banyan.test.contract.JsonCodecContract;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Jackson 默认实现的契约测试：继承 {@link JsonCodecContract}，再补充实现相关的用例。
 *
 * @author kennylee
 * @since 0.1.0
 */
class JsonCodecContractTest extends JsonCodecContract {

    private final JsonCodec codec = new JacksonJsonCodec();

    @Override
    protected String toJson(Object value) {
        return codec.toJson(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object fromJson(String json, Class<?> type) {
        return codec.fromJson(json, (Class<Object>) type);
    }

    /** 忽略未知字段。 */
    @Test
    void ignoresUnknownFields() {
        Foo foo = codec.fromJson("{\"a\":1,\"b\":2}", Foo.class);

        assertEquals(1, foo.getA());
    }

    /** 反序列化泛型列表。 */
    @Test
    @SuppressWarnings("unchecked")
    void deserializesGenericList() {
        Object result = codec.fromJson("[{\"a\":1},{\"a\":2}]",
                new com.fasterxml.jackson.core.type.TypeReference<List<Foo>>() {
                }.getType());

        List<Foo> list = (List<Foo>) result;
        assertEquals(2, list.size());
        assertEquals(2, list.get(1).getA());
    }

    public static class Foo {
        private int a;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }
    }
}
