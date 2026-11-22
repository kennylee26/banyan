package cn.kennylee.banyan.mq.core;

import java.util.Map;

/**
 * 消费端拆分后的消息属性：普通 headers、扩展信息与发送时间戳。
 *
 * @param headers   普通消息头
 * @param extra     扩展信息
 * @param timestamp 发送时间戳，可能为 null
 * @author kennylee
 * @since 0.1.0
 */
public record MqMessageAttributes(Map<String, String> headers, Map<String, String> extra,
                                  Long timestamp) {
}
