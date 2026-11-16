package cn.kennylee.banyan.commons.error;

/**
 * 消息解析器。默认用 JDK ResourceBundle，Spring 环境可替换为 MessageSource 实现。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface MessageResolver {

    /**
     * 按 key 解析消息并填充占位符。
     *
     * @param key  消息 key
     * @param args 占位符参数
     * @return 解析后的消息；key 不存在时返回 key 本身
     */
    String resolve(String key, Object... args);
}
