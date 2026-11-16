package cn.kennylee.banyan.commons.spring.json;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Type;

/**
 * 基于 Jackson 的默认实现。契约：null 原样、java.time 用 ISO 文本、BigDecimal 保精度、未知字段忽略。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class JacksonJsonCodec implements JsonCodec {

    /** Jackson ObjectMapper。 */
    private final ObjectMapper mapper;

    /**
     * 构造实例。
     */
    public JacksonJsonCodec() {
        this(defaultMapper());
    }

    /**
     * 构造实例。
     *
     * @param mapper 底层 Mapper
     */
    public JacksonJsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** 构造默认 ObjectMapper。 */
    private static ObjectMapper defaultMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * 序列化为 JSON 文本。
     *
     * @param value 值
     * @return 字符串
     */
    @Override
    public String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BanyanException(ErrorCode.JSON_SERIALIZE_FAILED, e);
        }
    }

    /**
     * 反序列化为对象。
     *
     * @param json JSON 文本
     * @param type 目标类型
     * @return 实体
     */
    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BanyanException(ErrorCode.JSON_DESERIALIZE_FAILED, e);
        }
    }

    /**
     * 反序列化为对象。
     *
     * @param json JSON 文本
     * @param type 目标类型
     * @return 结果对象
     */
    @Override
    public Object fromJson(String json, Type type) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        } catch (JsonProcessingException e) {
            throw new BanyanException(ErrorCode.JSON_DESERIALIZE_FAILED, e);
        }
    }
}
