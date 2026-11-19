package cn.kennylee.banyan.log.lifecycle;

/**
 * 运行时环境信息快照。取不到的字段为 null，由输出层显示 N/A。
 *
 * @param applicationName 应用名
 * @param profiles        active profiles，逗号分隔
 * @param webContainer    Web 容器类型与版本
 * @param servletVersion  Servlet 规范版本
 * @param cpuCores        CPU 核数
 * @param javaVersion     Java 版本
 * @param vmVersion       VM 名称与版本
 * @param timeZone        默认时区
 * @param maxMemory       最大内存（字节）
 * @param totalMemory     总内存（字节）
 * @param freeMemory      空闲内存（字节）
 * @param usableMemory    可用内存（字节）
 * @author kennylee
 * @since 0.1.0
 */
public record EnvironmentInfo(String applicationName, String profiles, String webContainer,
                              String servletVersion, int cpuCores, String javaVersion,
                              String vmVersion, String timeZone, long maxMemory,
                              long totalMemory, long freeMemory, long usableMemory) {
}
