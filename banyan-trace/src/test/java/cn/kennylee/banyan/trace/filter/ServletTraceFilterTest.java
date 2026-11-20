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
 * Servlet 入向过滤器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TraceTestApplication.class)
@AutoConfigureMockMvc
class ServletTraceFilterTest {

    @Autowired
    private MockMvc mockMvc;

    /** 沿用上游 TraceId，写入上下文、MDC 与响应头。 */
    @Test
    void reusesIncomingTraceId() throws Exception {
        // when
        mockMvc.perform(get("/trace").header("X-Trace-Id", "upstream-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "upstream-1"))
                .andExpect(jsonPath("$.context").value("upstream-1"))
                .andExpect(jsonPath("$.mdc").value("upstream-1"));
    }

    /** 没有上游值时生成新值。 */
    @Test
    void generatesTraceIdWhenMissing() throws Exception {
        // when
        mockMvc.perform(get("/trace"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")))
                .andExpect(jsonPath("$.context").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")))
                .andExpect(jsonPath("$.mdc").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")));
    }
}
