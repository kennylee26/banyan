/**
 * 纯 JDK 的签名与防重放工具：规范化、HMAC-SHA256、密钥、时间窗口与 nonce 去重。
 *
 * <p>零第三方、零 Spring、零 HTTP 耦合；只收普通字符串与 Map，由使用方在自己的
 * HTTP 层组装。包含 HMAC-SHA256 签名、AES-256-GCM 请求体加密与防重放；
 * 不提供自动装配，注解接入见 {@code banyan-openapi-sign}。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
package cn.kennylee.banyan.commons.crypto;
