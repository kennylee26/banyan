package cn.kennylee.banyan.log.lifecycle;

import cn.kennylee.banyan.commons.util.Strings;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TimeZone;

/**
 * 纯 JDK 的默认实现：运行时信息来自 {@link Runtime} / {@link System}，
 * 容器类型与版本通过反射探测，不 compile 依赖任何容器或 Servlet 实现。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class JdkEnvironmentInfoProvider implements EnvironmentInfoProvider {

    /** 应用名属性。 */
    private static final String APP_NAME_PROPERTY = "spring.application.name";

    /** 容器名到版本探测类的映射。 */
    private static final Map<String, String> VERSION_PROBES = Map.of(
            "Tomcat", "org.apache.catalina.startup.Tomcat",
            "Netty", "io.netty.util.NettyRuntime",
            "Undertow", "io.undertow.Undertow");

    /** WebServer 类名后缀。 */
    private static final String WEB_SERVER_SUFFIX = "WebServer";

    /** 版本探测类映射。 */
    private final Map<String, String> versionProbes;

    /**
     * 构造实例，使用内置的容器版本探测表。
     */
    public JdkEnvironmentInfoProvider() {
        this(VERSION_PROBES);
    }

    /**
     * 构造实例，供测试注入探测表。
     *
     * @param versionProbes 容器名到探测类的映射
     */
    JdkEnvironmentInfoProvider(Map<String, String> versionProbes) {
        this.versionProbes = versionProbes;
    }

    @Override
    public EnvironmentInfo collect(ApplicationContext context) {
        Environment environment = context.getEnvironment();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return new EnvironmentInfo(
                trimToNull(environment.getProperty(APP_NAME_PROPERTY)),
                joinProfiles(environment.getActiveProfiles()),
                containerDescription(context),
                servletVersion(context),
                runtime.availableProcessors(),
                System.getProperty("java.version"),
                System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"),
                TimeZone.getDefault().getID(),
                maxMemory,
                totalMemory,
                freeMemory,
                maxMemory - (totalMemory - freeMemory));
    }

    /** 容器类型与版本，非 Web 应用返回 null。 */
    private String containerDescription(ApplicationContext context) {
        if (!(context instanceof WebServerApplicationContext webContext)) {
            return null;
        }
        String simpleName = webContext.getWebServer().getClass().getSimpleName();
        String container = simpleName.replace(WEB_SERVER_SUFFIX, "");
        String version = detectVersion(container);
        return Strings.isBlank(version) ? container : container + " " + version;
    }

    /**
     * 反射读取容器实现版本。
     *
     * @param container 容器名
     * @return 版本；探测类缺失或未声明版本时返回 null
     */
    String detectVersion(String container) {
        String probe = versionProbes.get(container);
        if (probe == null) {
            return null;
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = JdkEnvironmentInfoProvider.class.getClassLoader();
            }
            Package probePackage = loader.loadClass(probe).getPackage();
            return probePackage == null ? null : probePackage.getImplementationVersion();
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /**
     * 反射读取 Servlet 规范版本，避免 compile 依赖 servlet-api。
     *
     * @param context 应用上下文
     * @return 形如 {@code 6.0} 的版本；非 Servlet 环境返回 null
     */
    String servletVersion(ApplicationContext context) {
        try {
            Object servletContext = context.getClass().getMethod("getServletContext").invoke(context);
            if (servletContext == null) {
                return null;
            }
            Method major = servletContext.getClass().getMethod("getMajorVersion");
            Method minor = servletContext.getClass().getMethod("getMinorVersion");
            return major.invoke(servletContext) + "." + minor.invoke(servletContext);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** 拼接 active profiles。 */
    private String joinProfiles(String[] profiles) {
        return profiles.length == 0 ? null : String.join(",", profiles);
    }

    /** 空白转 null。 */
    private String trimToNull(String value) {
        return Strings.isBlank(value) ? null : value.trim();
    }
}
