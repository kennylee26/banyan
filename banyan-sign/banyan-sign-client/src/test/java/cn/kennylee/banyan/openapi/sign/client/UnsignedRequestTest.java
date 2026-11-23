package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 待签名请求校验测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class UnsignedRequestTest {

    /** URI 没有 path 时按服务端的根路径 "/" 处理。 */
    @Test
    void normalizesEmptyPathToRoot() {
        // when
        UnsignedRequest request = new UnsignedRequest("GET", "", Map.of(), null);

        // then
        assertEquals("/", request.path());
    }

    /** 缺少 path 或 method 时失败。 */
    @Test
    void rejectsMissingMethodOrPath() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> new UnsignedRequest(null, "/openapi/ping", Map.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new UnsignedRequest("GET", null, Map.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new UnsignedRequest("GET", "  ", Map.of(), null));
    }
}
