package cn.kennylee.banyan.log.lifecycle;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.log.autoconfigure.LogProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 生命周期日志：启动横幅与环境快照、优雅停机、启动失败。
 *
 * <p>只监听 Spring 事件；不改 JVM 默认异常处理器、不注册 OOM hook、
 * 不用跨上下文共享的静态状态，停机去重状态属于当前实例。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class LifecycleLogger {

    /** 取不到的值。 */
    private static final String NOT_AVAILABLE = "N/A";

    /** 字节到 MB 的换算基数。 */
    private static final long BYTES_PER_MB = 1024L * 1024L;

    /** 应用名属性。 */
    private static final String APP_NAME_PROPERTY = "spring.application.name";

    /** 配置。 */
    private final LogProperties properties;

    /** 环境信息采集器。 */
    private final EnvironmentInfoProvider environmentInfoProvider;

    /** Spring 环境。 */
    private final Environment environment;

    /** 停机日志是否已输出（实例级，不跨上下文共享）。 */
    private final AtomicBoolean shutdownLogged = new AtomicBoolean();

    /**
     * 构造实例。
     *
     * @param properties               配置
     * @param environmentInfoProvider  环境信息采集器
     * @param environment              Spring 环境
     */
    public LifecycleLogger(LogProperties properties, EnvironmentInfoProvider environmentInfoProvider,
                           Environment environment) {
        this.properties = properties;
        this.environmentInfoProvider = environmentInfoProvider;
        this.environment = environment;
    }

    /**
     * 启动完成：输出横幅，按开关输出环境快照。
     *
     * @param event 启动完成事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        ApplicationContext context = event.getApplicationContext();
        String applicationName = applicationName(context);
        String profiles = profiles(context);
        log.info(banner(applicationName, profiles));
        if (!properties.getLifecycle().isEnvironmentEnabled()) {
            return;
        }
        EnvironmentInfo info = environmentInfoProvider.collect(context);
        log.info(Messages.get(ErrorCode.LOG_ENV_TITLE));
        log.info(Messages.get(ErrorCode.LOG_ENV_RUNTIME, text(info.applicationName()),
                text(info.profiles()), text(info.webContainer()), text(info.servletVersion())));
        log.info(Messages.get(ErrorCode.LOG_ENV_PLATFORM, info.cpuCores(), text(info.javaVersion()),
                text(info.vmVersion()), text(info.timeZone())));
        log.info(Messages.get(ErrorCode.LOG_ENV_MEMORY, toMegabytes(info.maxMemory()),
                toMegabytes(info.totalMemory()), toMegabytes(info.freeMemory()),
                toMegabytes(info.usableMemory())));
    }

    /**
     * 上下文关闭：输出一次优雅停机日志。
     *
     * @param event 上下文关闭事件
     */
    @EventListener(ContextClosedEvent.class)
    public void onClosed(ContextClosedEvent event) {
        if (shutdownLogged.compareAndSet(false, true)) {
            log.info(Messages.get(ErrorCode.LOG_SHUTDOWN, applicationName(event.getApplicationContext())));
        }
    }

    /**
     * 启动失败：输出错误日志并保留异常堆栈。
     *
     * @param event 启动失败事件
     */
    @EventListener(ApplicationFailedEvent.class)
    public void onFailure(ApplicationFailedEvent event) {
        log.error(Messages.get(ErrorCode.LOG_STARTUP_FAILED,
                applicationName(event.getApplicationContext())), event.getException());
    }

    /** 应用名，取不到显示 N/A。 */
    private String applicationName(ApplicationContext context) {
        return text(context.getEnvironment().getProperty(APP_NAME_PROPERTY));
    }

    /** active profiles，取不到显示 N/A。 */
    private String profiles(ApplicationContext context) {
        String[] profiles = context.getEnvironment().getActiveProfiles();
        return text(profiles.length == 0 ? null : String.join(",", profiles));
    }

    /** 横幅：配置优先，缺省回落消息资源。 */
    private String banner(String applicationName, String profiles) {
        String custom = properties.getLifecycle().getBanner();
        String template = Strings.isBlank(custom) ? Messages.get(ErrorCode.LOG_BANNER) : custom;
        return MessageFormat.format(template, applicationName, profiles);
    }

    /** 空白转 N/A。 */
    private String text(String value) {
        return Strings.isBlank(value) ? NOT_AVAILABLE : value;
    }

    /**
     * 字节转 MB。
     *
     * @param bytes 字节数
     * @return MB 数
     */
    long toMegabytes(long bytes) {
        return bytes / BYTES_PER_MB;
    }
}
