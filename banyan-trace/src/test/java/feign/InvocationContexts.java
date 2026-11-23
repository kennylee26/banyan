package feign;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * 测试用 InvocationContext 工厂；构造器是包私有，只能放在 feign 包内。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class InvocationContexts {

    private InvocationContexts() {
    }

    /**
     * 构造上下文。
     *
     * @param headers 响应头
     * @return 上下文
     */
    public static InvocationContext of(Map<String, java.util.Collection<String>> headers) {
        Request request = Request.create(Request.HttpMethod.GET, "/hello",
                Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8,
                new RequestTemplate());
        Response response = Response.builder().status(200).reason("OK").request(request)
                .headers(headers).body("ok", java.nio.charset.StandardCharsets.UTF_8).build();
        return new InvocationContext("GET", null, null,
                false, false, false, response, (Type) String.class);
    }
}
