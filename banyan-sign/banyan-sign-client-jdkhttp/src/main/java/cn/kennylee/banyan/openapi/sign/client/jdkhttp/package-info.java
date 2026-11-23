/**
 * JDK HttpClient 适配器：把已构造的 {@code HttpRequest} 转换为已签名请求。
 *
 * <p>JDK HttpClient 没有请求拦截器 API，{@code HttpRequest} 也不可变，因此本包提供
 * 「传入请求 + body，返回新的已签名请求」的用法，不包装 HttpClient。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
package cn.kennylee.banyan.openapi.sign.client.jdkhttp;
