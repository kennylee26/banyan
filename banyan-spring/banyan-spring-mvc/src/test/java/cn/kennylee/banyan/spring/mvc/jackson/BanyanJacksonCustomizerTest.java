package cn.kennylee.banyan.spring.mvc.jackson;

import cn.kennylee.banyan.spring.mvc.autoconfigure.MvcProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可选 Jackson 增强测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class BanyanJacksonCustomizerTest {

    /** 默认开关下 Long 转字符串、非 Long 数字不变、BigDecimal 不用科学计数法。 */
    @Test
    void appliesDefaultConvenienceSwitches() throws Exception {
        // given
        ObjectMapper mapper = mapperFor(new MvcProperties.Jackson());
        Sample sample = new Sample(1L, 2, new BigDecimal("1E+3"));

        // when
        String json = mapper.writeValueAsString(sample);

        // then
        assertThat(json).contains("\"longValue\":\"1\"");
        assertThat(json).contains("\"intValue\":2");
        assertThat(json).contains("\"bigValue\":1000");
    }

    /** 关闭 Long 转字符串后仍按数字输出。 */
    @Test
    void longAsStringCanBeDisabled() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setLongAsString(false);
        ObjectMapper mapper = mapperFor(options);

        // when
        String json = mapper.writeValueAsString(new Sample(1L, 2, BigDecimal.ONE));

        // then
        assertThat(json).contains("\"longValue\":1");
    }

    /** 关闭枚举按 name 后按 toString 输出。 */
    @Test
    void enumsUsingNameCanBeDisabled() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setEnumsUsingName(false);
        ObjectMapper mapper = mapperFor(options);

        // when
        String json = mapper.writeValueAsString(new EnumHolder(TestColor.RED));

        // then
        assertThat(json).contains("\"red\"");
    }

    /** 默认按枚举 name 输出。 */
    @Test
    void enumsUseNameByDefault() throws Exception {
        // given
        ObjectMapper mapper = mapperFor(new MvcProperties.Jackson());

        // when
        String json = mapper.writeValueAsString(new EnumHolder(TestColor.RED));

        // then
        assertThat(json).contains("\"RED\"");
    }

    /** 默认配置下 Date 与 LocalDateTime 都按统一格式输出。 */
    @Test
    void formatsDateAndLocalDateTime() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setTimeZone("UTC");
        ObjectMapper mapper = mapperFor(options);
        DateSample sample = new DateSample(new Date(0L), LocalDateTime.of(2026, 11, 25, 10, 30, 0),
                LocalDate.of(2026, 11, 25));

        // when
        String json = mapper.writeValueAsString(sample);

        // then
        assertThat(json).contains("\"date\":\"1970-01-01 00:00:00\"");
        assertThat(json).contains("\"dateTime\":\"2026-11-25 10:30:00\"");
        assertThat(json).contains("\"localDate\":\"2026-11-25\"");
    }

    /** LocalDateTime 能按配置的格式反序列化。 */
    @Test
    void deserializesLocalDateTimeWithConfiguredFormat() throws Exception {
        // given
        ObjectMapper mapper = mapperFor(new MvcProperties.Jackson());
        String json = "{\"dateTime\":\"2026-11-25 10:30:00\"}";

        // when
        DateSample sample = mapper.readValue(json, DateSample.class);

        // then
        assertThat(sample.dateTime()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 30, 0));
    }

    /** LocalDateTime 可按多个候选格式解析。 */
    @Test
    void deserializesLocalDateTimeWithMultipleFormats() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setDateFormats(List.of("yyyy/MM/dd HH:mm:ss", "yyyyMMddHHmmss"));
        ObjectMapper mapper = mapperFor(options);

        // when
        DateSample slash = mapper.readValue("{\"dateTime\":\"2026/11/25 10:30:00\"}", DateSample.class);
        DateSample compact = mapper.readValue("{\"dateTime\":\"20261125103000\"}", DateSample.class);

        // then
        LocalDateTime expected = LocalDateTime.of(2026, 11, 25, 10, 30, 0);
        assertThat(slash.dateTime()).isEqualTo(expected);
        assertThat(compact.dateTime()).isEqualTo(expected);
    }

    /** LocalDate 可按多个候选格式解析。 */
    @Test
    void deserializesLocalDateWithMultipleFormats() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setLocalDateFormats(List.of("yyyy/MM/dd", "yyyyMMdd"));
        ObjectMapper mapper = mapperFor(options);

        // when
        DateSample slash = mapper.readValue("{\"localDate\":\"2026/11/25\"}", DateSample.class);
        DateSample compact = mapper.readValue("{\"localDate\":\"20261125\"}", DateSample.class);

        // then
        LocalDate expected = LocalDate.of(2026, 11, 25);
        assertThat(slash.localDate()).isEqualTo(expected);
        assertThat(compact.localDate()).isEqualTo(expected);
    }

    /** 布尔字段接受布尔值、数字 0/1 与字符串 true/false/0/1。 */
    @Test
    void parsesLenientBoolean() throws Exception {
        // given
        ObjectMapper mapper = mapperFor(new MvcProperties.Jackson());

        // when / then
        assertThat(mapper.readValue("{\"flag\":true}", BooleanSample.class).flag()).isTrue();
        assertThat(mapper.readValue("{\"flag\":false}", BooleanSample.class).flag()).isFalse();
        assertThat(mapper.readValue("{\"flag\":1}", BooleanSample.class).flag()).isTrue();
        assertThat(mapper.readValue("{\"flag\":0}", BooleanSample.class).flag()).isFalse();
        assertThat(mapper.readValue("{\"flag\":\"true\"}", BooleanSample.class).flag()).isTrue();
        assertThat(mapper.readValue("{\"flag\":\"false\"}", BooleanSample.class).flag()).isFalse();
        assertThat(mapper.readValue("{\"flag\":\"1\"}", BooleanSample.class).flag()).isTrue();
        assertThat(mapper.readValue("{\"flag\":\"0\"}", BooleanSample.class).flag()).isFalse();
    }

    /** 日期格式置空时不处理 Date。 */
    @Test
    void dateFormatCanBeDisabled() throws Exception {
        // given
        MvcProperties.Jackson options = new MvcProperties.Jackson();
        options.setDateFormat("");
        ObjectMapper mapper = mapperFor(options);

        // when
        String json = mapper.writeValueAsString(new DateSample(new Date(0L), null, null));

        // then
        assertThat(json).contains("\"date\":0");
    }

    /** 排序晚于 Spring Boot 自身 customizer（order=0）。 */
    @Test
    void orderIsAfterBootCustomizer() {
        // given
        BanyanJacksonCustomizer customizer = new BanyanJacksonCustomizer(new MvcProperties.Jackson());

        // when
        int order = customizer.getOrder();

        // then
        assertThat(order).isGreaterThan(0);
        assertThat(order).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private static ObjectMapper mapperFor(MvcProperties.Jackson options) {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new BanyanJacksonCustomizer(options).customize(builder);
        return builder.build();
    }

    /**
     * 测试用样本。
     *
     * @param longValue Long 字段
     * @param intValue  Integer 字段
     * @param bigValue  BigDecimal 字段
     * @author kennylee
     * @since 0.1.0
     */
    record Sample(Long longValue, Integer intValue, BigDecimal bigValue) {
    }

    /**
     * 测试用枚举容器。
     *
     * @param color 颜色
     * @author kennylee
     * @since 0.1.0
     */
    record EnumHolder(TestColor color) {
    }

    /**
     * 测试用日期容器。
     *
     * @param date      旧日期类型
     * @param dateTime  新日期时间类型
     * @param localDate 仅日期类型
     * @author kennylee
     * @since 0.1.0
     */
    record DateSample(Date date, LocalDateTime dateTime, LocalDate localDate) {
    }

    /**
     * 测试用布尔容器。
     *
     * @param flag 布尔值
     * @author kennylee
     * @since 0.1.0
     */
    record BooleanSample(boolean flag) {
    }

    /**
     * 测试用枚举，toString 与 name 不同。
     *
     * @author kennylee
     * @since 0.1.0
     */
    enum TestColor {

        RED;

        @Override
        public String toString() {
            return "red";
        }
    }
}
