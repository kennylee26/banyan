package cn.kennylee.banyan.log.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 纯 JDK 环境信息采集测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class EnvironmentInfoProviderTest {

    private final JdkEnvironmentInfoProvider provider = new JdkEnvironmentInfoProvider();

    /** 采集运行时信息。 */
    @Test
    void collectsRuntimeInfo() {
        // given
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "order");
        environment.setActiveProfiles("dev", "prod");
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);

        // when
        EnvironmentInfo info = provider.collect(context);

        // then
        assertThat(info.applicationName()).isEqualTo("order");
        assertThat(info.profiles()).isEqualTo("dev,prod");
        assertThat(info.cpuCores()).isPositive();
        assertThat(info.javaVersion()).isNotBlank();
        assertThat(info.vmVersion()).isNotBlank();
        assertThat(info.timeZone()).isNotBlank();
        assertThat(info.maxMemory()).isPositive();
        assertThat(info.totalMemory()).isPositive();
        assertThat(info.freeMemory()).isNotNegative();
        assertThat(info.usableMemory())
                .isEqualTo(info.maxMemory() - (info.totalMemory() - info.freeMemory()));
        assertThat(info.webContainer()).isNull();
        assertThat(info.servletVersion()).isNull();
    }

    /** 没有应用名时不报错。 */
    @Test
    void toleratesMissingApplicationName() {
        // given
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(new MockEnvironment());

        // when
        assertThat(provider.collect(context).applicationName()).isNull();
    }

    /** 单个 profile 原样输出。 */
    @Test
    void keepsSingleProfile() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);

        // when
        assertThat(provider.collect(context).profiles()).isEqualTo("dev");
    }

    /** 探测类不存在时版本为 null。 */
    @Test
    void detectVersionReturnsNullWhenProbeMissing() {
        // given
        JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(
                Map.of("Tomcat", "does.not.Exist"));

        // when
        assertThat(custom.detectVersion("Tomcat")).isNull();
    }

    /** 容器名不在探测表时版本为 null。 */
    @Test
    void detectVersionReturnsNullForUnknownContainer() {
        // when
        assertThat(provider.detectVersion("Unknown")).isNull();
    }

    /** 探测类存在但未声明实现版本时为 null。 */
    @Test
    void detectVersionReturnsNullWhenPackageHasNoVersion() {
        // given
        JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(
                Map.of("Demo", "java.lang.String"));

        // when
        assertThat(custom.detectVersion("Demo")).isNull();
    }

    /** 探测类存在且声明版本时返回版本。 */
    @Test
    void detectVersionReadsImplementationVersion() {
        // given
        JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(
                Map.of("Demo", "ch.qos.logback.classic.Logger"));

        // when
        assertThat(custom.detectVersion("Demo")).isNotBlank();
    }

    /** 使用当前线程上下文类加载器探测；它加载不到时不回退到其它加载器。 */
    @Test
    void detectVersionUsesContextClassLoader() {
        // given
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(new ClassLoader(null) {
            });
            JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(
                    Map.of("Demo", "ch.qos.logback.classic.Logger"));

            // when
            assertThat(custom.detectVersion("Demo")).isNull();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    /** 上下文类加载器为空时回退到本类加载器。 */
    @Test
    void detectVersionFallsBackWhenContextClassLoaderMissing() {
        // given
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(null);
            JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(
                    Map.of("Demo", "ch.qos.logback.classic.Logger"));

            // when
            assertThat(custom.detectVersion("Demo")).isNotBlank();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    /** 探测时不初始化目标类。 */
    @Test
    void detectVersionDoesNotInitializeProbeClass() {
        // given
        ProbeWithInitializer.INITIALIZED = false;
        JdkEnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider(Map.of(
                "Demo", "cn.kennylee.banyan.log.lifecycle.EnvironmentInfoProviderTest$ProbeWithInitializer"));

        // when
        custom.detectVersion("Demo");

        // then
        assertThat(ProbeWithInitializer.INITIALIZED).isFalse();
    }

    /** Web 上下文输出容器类型。 */
    @Test
    void describesContainerFromWebContext() {
        // given
        WebServerApplicationContext context = mock(WebServerApplicationContext.class);
        when(context.getEnvironment()).thenReturn(new MockEnvironment()
                .withProperty("spring.application.name", "order"));
        when(context.getWebServer()).thenReturn(new FakeWebServer());

        // when
        assertThat(provider.collect(context).webContainer()).isEqualTo("Fake");
    }

    /** Servlet 环境读取规范版本。 */
    @Test
    void readsServletVersion() {
        // given
        FakeServletWebContext context = new FakeServletWebContext();
        context.setEnvironment(new MockEnvironment());

        // when
        assertThat(provider.servletVersion(context)).isEqualTo("6.0");
    }

    /** 没有 getServletContext 方法时返回 null。 */
    @Test
    void servletVersionReturnsNullWithoutMethod() {
        // given
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(new MockEnvironment());

        // when
        assertThat(provider.servletVersion(context)).isNull();
    }

    /** getServletContext 返回 null 时返回 null。 */
    @Test
    void servletVersionReturnsNullWhenContextAbsent() {
        // given
        NullServletWebContext context = new NullServletWebContext();
        context.setEnvironment(new MockEnvironment());

        // when
        assertThat(provider.servletVersion(context)).isNull();
    }

    /** 带静态初始化的探测类。 */
    public static class ProbeWithInitializer {

        /** 是否已被初始化。 */
        static boolean INITIALIZED;

        static {
            INITIALIZED = true;
        }
    }

    /** 测试用 WebServer。 */
    public static class FakeWebServer implements WebServer {

        @Override
        public int getPort() {
            return 8080;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }

    /** 测试用 Servlet 上下文。 */
    public static class FakeServletWebContext extends GenericApplicationContext {

        /** 模拟的 ServletContext。 */
        private final FakeServletContextApi servletContext = new FakeServletContextApi();

        /**
         * 返回模拟 ServletContext。
         *
         * @return ServletContext
         */
        public FakeServletContextApi getServletContext() {
            return servletContext;
        }
    }

    /** 测试用 ServletContext API。 */
    public static class FakeServletContextApi {

        /**
         * 主版本。
         *
         * @return 主版本
         */
        public int getMajorVersion() {
            return 6;
        }

        /**
         * 次版本。
         *
         * @return 次版本
         */
        public int getMinorVersion() {
            return 0;
        }
    }

    /** getServletContext 返回 null 的上下文。 */
    public static class NullServletWebContext extends GenericApplicationContext {

        /**
         * 模拟缺失的 ServletContext。
         *
         * @return null
         */
        public Object getServletContext() {
            return null;
        }
    }
}
