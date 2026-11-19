package cn.kennylee.banyan.log.template;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * logback 模板回归测试：片段可解析、不越权、不夹带公司内容。
 *
 * @author kennylee
 * @since 0.1.0
 */
class LogbackTemplateTest {

    /** 主片段。 */
    private static final String MAIN = "banyan/logback/banyan-logback-spring.xml";

    /** 三个 appender 片段。 */
    private static final List<String> APPENDERS = List.of(
            "banyan/logback/console-appender.xml",
            "banyan/logback/file-daily-appender.xml",
            "banyan/logback/error-appender.xml");

    /** 可选 root 片段。 */
    private static final String ROOT = "banyan/logback/banyan-logback-root.xml";

    /** `<springProperty .../>` 标签。 */
    private static final Pattern SPRING_PROPERTY = Pattern.compile("<springProperty\\b([^>]*)/>");

    /** XML 属性。 */
    private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    /** 所有片段都是合法 XML。 */
    @Test
    void allFragmentsAreWellFormedXml() throws Exception {
        for (String resource : allResources()) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(read(resource).getBytes(StandardCharsets.UTF_8)));
        }
    }

    /** 主片段与 appender 片段不越权接管 root / logger / contextName。 */
    @Test
    void fragmentsDoNotOwnHostConfiguration() throws IOException {
        for (String resource : List.of(MAIN, APPENDERS.get(0), APPENDERS.get(1), APPENDERS.get(2))) {
            String text = read(resource);
            assertThat(text).as(resource)
                    .doesNotContain("<root").doesNotContain("<logger").doesNotContain("<contextName");
        }
    }

    /** 不夹带公司命名、APM 占位与 janino 条件。 */
    @Test
    void fragmentsCarryNoCompanyContent() throws IOException {
        for (String resource : allResources()) {
            String text = read(resource);
            assertThat(text).as(resource)
                    .doesNotContain("wuyang")
                    .doesNotContain("apm-traceid")
                    .doesNotContain("%esb{APPLICATION")
                    .doesNotContain("<if>");
        }
    }

    /** 主片段使用 banyan 属性前缀并保留可配置的 traceId 占位。 */
    @Test
    void mainFragmentUsesBanyanPrefixAndTracePlaceholder() throws IOException {
        String text = read(MAIN);

        assertThat(text).contains("banyan.log.logback.").contains("TRACE_ID_KEY")
                .contains("[traceId=%X{").contains("N/A");
    }

    /** 主片段不重定义宿主的 pattern 与 LOG_FILE 变量。 */
    @Test
    void mainFragmentDoesNotRedefineHostVariables() throws IOException {
        String text = read(MAIN);

        assertThat(text)
                .doesNotContain("<property name=\"CONSOLE_LOG_PATTERN\"")
                .doesNotContain("<property name=\"FILE_LOG_PATTERN\"")
                .doesNotContain("<property name=\"LOG_FILE\"")
                .contains("BANYAN_LOG_FILE");
    }

    /** appender 片段可被 Joran 解析。 */
    @Test
    void appenderFragmentsParseWithJoran() throws Exception {
        for (String resource : APPENDERS) {
            configure(wrapForJoran(read(resource)));
        }
    }

    /** 主片段去掉 Spring 专属标签后可被 Joran 解析。 */
    @Test
    void mainFragmentParsesWithJoranAfterConvertingSpringProperties() throws Exception {
        configure(convertSpringProperties(read(MAIN)));
    }

    /** 读取全部片段资源。 */
    private List<String> allResources() {
        return List.of(MAIN, APPENDERS.get(0), APPENDERS.get(1), APPENDERS.get(2), ROOT);
    }

    /** 从测试 classpath 读取片段。 */
    private String read(String resource) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as(resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 去掉 `<included>` 根标签并补上片段依赖的变量。 */
    private String wrapForJoran(String fragment) {
        String body = stripProlog(fragment)
                .replaceFirst("<included>", "")
                .replaceFirst("</included>", "");
        return "<configuration>"
                + "<include resource=\"org/springframework/boot/logging/logback/defaults.xml\"/>"
                + "<property name=\"BANYAN_LOG_FILE\" value=\"target/test.log\"/>"
                + "<property name=\"BANYAN_LOG_MAX_SIZE\" value=\"10MB\"/>"
                + "<property name=\"BANYAN_LOG_MAX_HISTORY\" value=\"3\"/>"
                + "<property name=\"BANYAN_LOG_TOTAL_CAP\" value=\"1GB\"/>"
                + body
                + "</configuration>";
    }

    /** 把 Spring 专属标签替换为普通 property，便于用原生 Joran 校验结构。 */
    private String convertSpringProperties(String fragment) {
        String body = stripProlog(fragment)
                .replaceFirst("<included>", "")
                .replaceFirst("</included>", "");
        Matcher matcher = SPRING_PROPERTY.matcher(body);
        StringBuilder converted = new StringBuilder();
        while (matcher.find()) {
            Map<String, String> attributes = new LinkedHashMap<>();
            Matcher attribute = ATTRIBUTE.matcher(matcher.group(1));
            while (attribute.find()) {
                attributes.put(attribute.group(1), attribute.group(2));
            }
            String replacement = "<property name=\"" + attributes.get("name")
                    + "\" value=\"" + attributes.getOrDefault("defaultValue", "") + "\"/>";
            matcher.appendReplacement(converted, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(converted);
        return "<configuration>" + converted + "</configuration>";
    }

    /** 去掉 XML 声明，避免嵌入包装根标签后非法。 */
    private String stripProlog(String fragment) {
        return fragment.replaceFirst("^\\s*<\\?xml[^>]*\\?>", "");
    }

    /** 用 Joran 解析一段配置。 */
    private void configure(String xml) throws Exception {
        LoggerContext context = new LoggerContext();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } finally {
            context.stop();
        }
    }
}
