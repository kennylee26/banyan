package cn.kennylee.banyan.trace.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Servlet 过滤器的 query 兜底与响应头开关测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TraceTestApplication.class, properties = {
        "banyan.trace.accept-query-trace-id=true",
        "banyan.trace.response-header.enabled=false"})
@AutoConfigureMockMvc
class ServletTraceFilterQueryTest {

    @Autowired
    private MockMvc mockMvc;

    /** query 兜底生效，响应头关闭后不回写。 */
    @Test
    void acceptsQueryAndSkipsResponseHeader() throws Exception {
        // when
        mockMvc.perform(get("/trace").param("traceId", "query-1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Trace-Id"))
                .andExpect(jsonPath("$.context").value("query-1"))
                .andExpect(jsonPath("$.mdc").value("query-1"));
    }
}
