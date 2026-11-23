package cn.kennylee.banyan.openapi.sign.client;

import java.util.List;
import java.util.Map;

/**
 * 规范化输入：签名所需的全部字段，纯数据载体。
 *
 * @param method      请求方法
 * @param path        已剥离 basePath 的请求路径
 * @param queryParams 查询参数
 * @param body        参与摘要的请求体（加密模式下为密文）
 * @param timestamp   毫秒时间戳
 * @param nonce       一次性随机串
 * @param keyId       密钥标识
 * @param alg         算法标识
 * @author kennylee
 * @since 0.1.0
 */
record CanonicalRequest(String method, String path, Map<String, List<String>> queryParams,
                        String body, long timestamp, String nonce, String keyId, String alg) {
}
