package cn.kennylee.banyan.test.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JSON 编解码的抽象契约用例。任何实现继承本类并实现两个钩子即可跑同一套断言。
 * <p>本类不依赖任何具体实现类型，避免测试装备与实现模块形成循环依赖。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public abstract class JsonCodecContract {

    /**
     * 把对象序列化为 JSON。
     *
     * @param value 值
     * @return 返回结果
     */
    protected abstract String toJson(Object value);

    /**
     * 把 JSON 反序列化为对象。
     *
     * @param json JSON 文本
     * @param type 目标类型
     * @return 返回结果
     */
    protected abstract Object fromJson(String json, Class<?> type);

    @Test
    void serializesNull() {
        assertEquals("null", toJson(null));
    }

    @Test
    void deserializesNull() {
        assertNull(fromJson("null", Object.class));
    }

    @Test
    void roundTripsMap() {
        String json = toJson(Map.of("a", 1));

        assertEquals("{\"a\":1}", json);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) fromJson(json, Map.class);
        assertEquals(1, map.get("a"));
    }

    @Test
    void writesJavaTimeAsIsoText() {
        assertEquals("\"2024-01-02T03:04:05\"",
                toJson(LocalDateTime.of(2024, 1, 2, 3, 4, 5)));
    }

    @Test
    void keepsBigDecimalPrecision() {
        assertEquals("12345678901234567890.123",
                toJson(new BigDecimal("12345678901234567890.123")));
    }
}
