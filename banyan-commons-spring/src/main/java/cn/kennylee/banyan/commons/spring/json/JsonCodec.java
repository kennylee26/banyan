package cn.kennylee.banyan.commons.spring.json;

import java.lang.reflect.Type;

/**
 * JSON 编解码接口。窄接口：不暴露任何实现特有的配置能力。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface JsonCodec {

    /**
     * 把对象序列化为 JSON 文本。
     *
     * @param value 待序列化对象
     * @return JSON 文本；入参为 null 时返回 {@code "null"}
     */
    String toJson(Object value);

    /**
     * 把 JSON 反序列化为指定类型的对象。
     *
     * @param json JSON 文本
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 反序列化结果；JSON 为 {@code "null"} 时返回 null
     */
    <T> T fromJson(String json, Class<T> type);

    /**
     * 把 JSON 反序列化为带泛型信息的类型。
     *
     * @param json JSON 文本
     * @param type 目标类型（含泛型参数）
     * @return 反序列化结果
     */
    Object fromJson(String json, Type type);
}
