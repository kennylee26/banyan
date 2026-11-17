package cn.kennylee.banyan.trace.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * WebFlux 入向过滤器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = ReactiveTraceTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.web-application-type=reactive")
@AutoConfigureWebTestClient
class ReactiveTraceFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    /** 沿用上游 TraceId，写入响应头、exchange 属性与 Reactor 上下文。 */
    @Test
    void reusesIncomingTraceId() {
        webTestClient.get().uri("/reactive-trace").header("X-Trace-Id", "upstream-2").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", "upstream-2")
                .expectBody()
                .jsonPath("$.context").isEqualTo("upstream-2")
                .jsonPath("$.attribute").isEqualTo("upstream-2")
                .jsonPath("$.reactor").isEqualTo("upstream-2");
    }

    /** 没有上游值时生成新值，并在 Reactor 上下文中可见。 */
    @Test
    void generatesTraceIdWhenMissing() {
        webTestClient.get().uri("/reactive-trace").exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("X-Trace-Id", "[0-9a-f]{32}")
                .expectBody()
                .jsonPath("$.attribute").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}"))
                .jsonPath("$.reactor").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}"));
    }
}
