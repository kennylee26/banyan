package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原始 query 解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class QueryParamsTest {

    /** 同名多值与编码解码。 */
    @Test
    void parsesMultiValuesAndEncodedText() {
        // when
        Map<String, List<String>> params =
                QueryParams.parse("a=1&a=2&b=x+y&c=%E4%B8%AD");

        // then
        assertEquals(List.of("1", "2"), params.get("a"));
        assertEquals(List.of("x y"), params.get("b"));
        assertEquals(List.of("中"), params.get("c"));
    }

    /** 空 query 返回空映射。 */
    @Test
    void handlesEmptyQuery() {
        // when
        assertTrue(QueryParams.parse(null).isEmpty());
        assertTrue(QueryParams.parse("").isEmpty());
    }

    /** 非法编码立即失败。 */
    @Test
    void rejectsInvalidEncoding() {
        // when
        assertThrows(IllegalArgumentException.class, () -> QueryParams.parse("a=%ZZ"));
    }
}
