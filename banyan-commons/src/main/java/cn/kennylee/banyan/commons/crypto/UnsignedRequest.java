package cn.kennylee.banyan.commons.crypto;

import java.util.List;
import java.util.Map;

/**
 * 调用方尚未签名的请求：只需要业务请求本身，不需要理解签名协议的内部字段。
 *
 * @param method      HTTP 方法
 * @param path        请求路径
 * @param queryParams 查询参数，允许同名多值
 * @param body        请求体原文，可为 null
 * @author kennylee
 * @since 0.1.0
 */
public record UnsignedRequest(String method, String path,
                              Map<String, List<String>> queryParams, String body) {

    /**
     * 深拷贝查询参数，保证值对象创建后不被外部集合修改。
     */
    public UnsignedRequest {
        queryParams = SignRequest.copyParams(queryParams);
    }

    /**
     * 创建待签名请求。
     *
     * @param method      HTTP 方法
     * @param path        请求路径
     * @param queryParams 查询参数
     * @param body        请求体
     * @return 待签名请求
     */
    public static UnsignedRequest of(String method, String path,
                                     Map<String, List<String>> queryParams, String body) {
        return new UnsignedRequest(method, path, queryParams, body);
    }
}
