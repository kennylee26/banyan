# banyan-sign-openapi

注解驱动的 OpenAPI 入站保护：验签 → 解密 → 把明文交给 Controller，失败统一 401。

- 依赖 `banyan-commons` 的签名与 AES-256-GCM 能力
- 默认自动装配，`banyan.openapi-sign.enabled=false` 可整体关闭
- 缺 Servlet / Spring MVC 时跳过装配并打 WARN，应用正常启动
- 不新增第三方坐标：Servlet、Spring MVC、Lombok 全部 `provided + optional`
- 调用方工具见 [`../banyan-sign-client`](../banyan-sign-client/README.md)；
  不引入 banyan 组件时发给对方 [`第三方对接指南`](../banyan-sign-client/THIRD-PARTY-GUIDE.md)

## 快速开始

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-sign-openapi</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
banyan:
  openapi-sign:
    keys:
      app1: change-me
```

```java
@RestController
@RequestMapping("/openapi/orders")
@OpenApiSigned
public class OrderController {

    @PostMapping
    public Order create(@RequestBody CreateOrder body) {
        // 这里是解密后的明文 JSON
        return service.create(body);
    }
}
```

调用方按 `banyan-commons` 的协议签名并加密 body，见
[`../../banyan-commons/guides/crypto.md`](../../banyan-commons/guides/crypto.md) 第九节。

## 配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `banyan.openapi-sign.enabled` | `true` | 模块总开关 |
| `banyan.openapi-sign.encrypted` | `true` | 是否要求 body 加密；灰度期可设 `false` 只验签 |
| `banyan.openapi-sign.sign-algorithm` | `HmacSHA256` | 当前只支持该值，其它值启动失败 |
| `banyan.openapi-sign.cipher` | `aes-gcm` | 内置加密实现标识，其它值启动失败 |
| `banyan.openapi-sign.window` | `5m` | 验签时间窗口 |
| `banyan.openapi-sign.reveal-reason` | `false` | 是否在响应文案中返回具体失败原因 |
| `banyan.openapi-sign.headers.alg` | `X-Banyan-Alg` | 算法 header 名 |
| `banyan.openapi-sign.headers.key-id` | `X-Banyan-KeyId` | 密钥标识 header 名 |
| `banyan.openapi-sign.headers.timestamp` | `X-Banyan-Timestamp` | 时间戳 header 名 |
| `banyan.openapi-sign.headers.nonce` | `X-Banyan-Nonce` | nonce header 名 |
| `banyan.openapi-sign.headers.sign` | `X-Banyan-Sign` | 签名 header 名 |
| `banyan.openapi-sign.keys.<keyId>` | 无 | 密钥；仅建议用于演示与联调 |
| `banyan.openapi-sign.error.code` | `openapi.sign.invalid` | 响应错误码，不随原因变化 |
| `banyan.openapi-sign.error.message` | 空 | 通用失败文案；为空时取消息资源 |

五个 header 名必须与调用方一致。`keyId` 不要包含点号，否则属性绑定会歧义；
复杂场景改用自定义 `SignKeyResolver`。

自定义 header 名时，服务端与调用方使用同一组名字（加密与解密不额外增加 header）：

```yaml
banyan:
  openapi-sign:
    headers:
      alg: X-Acme-Alg
      key-id: X-Acme-KeyId
      timestamp: X-Acme-Timestamp
      nonce: X-Acme-Nonce
      sign: X-Acme-Sign
```

调用方对应写法：

```java
new SignHeaders("X-Acme-Alg", "X-Acme-KeyId", "X-Acme-Timestamp",
        "X-Acme-Nonce", "X-Acme-Sign");
```

## 失败响应与日志

默认响应固定为 401：

```json
{"code":"openapi.sign.invalid","message":"签名校验失败"}
```

- 错误签名、过期、重放、未知 `keyId`、解密失败默认返回相同 `code` 与 `message`
- `reveal-reason=true` 时 `message` 换成具体原因文案，`code` 不变；上线前记得关掉
- 日志始终 WARN，包含失败原因、method、path、keyId，不含密钥、签名串或 body

## 替换默认实现

声明同类型 bean 即可覆盖，配置中的默认实现会让位：

| 接口 | 默认实现 | 什么时候替换 |
|---|---|---|
| `SignKeyResolver` | 从 `keys` 配置读取 | 密钥来自配置中心或密钥服务 |
| `NonceStore` | `InMemoryNonceStore` | 多实例部署，必须共享去重 |
| `PayloadCipher` | `AesGcmPayloadCipher` | 需要 SM4 或其它算法 |
| `SignAlgorithm` | `HmacSha256SignAlgorithm` | 需要另一套签名算法 |

如果替换 `ReplayGuard` bean，必须使用与 `banyan.openapi-sign.headers.*` 相同的
`SignHeaders`，否则验签会读不到自定义 header 名。

多实例示例：

```java
@Bean
public NonceStore nonceStore(RedisTemplate<String, String> redis) {
    return (nonceKey, ttlMillis) -> Boolean.TRUE.equals(redis.opsForValue()
            .setIfAbsent(nonceKey, "1", Duration.ofMillis(ttlMillis)));
}
```

## 实现说明与限制

- `RepeatableBodyFilter` 缓存原始 body；拦截器验签成功后把明文写入请求属性，包装器读取时替换。
  因此下游 `@RequestBody` 与输入流都能读到明文。
- 过滤器会完整缓存非 `multipart/*` 的请求体，没有大小上限；大 body 接口请评估内存，或不要挂在这个应用上。
- `multipart/*` 文件上传直接跳过缓存；如果给这类接口标注 `@OpenApiSigned`，会因拿不到缓存 body 返回 401。
- 验签使用的 path 是「请求 URI 去掉 context-path」后的值；带 context-path 部署时调用方仍按业务 path 签名。
- 拦截器必须能在请求属性里拿到缓存 body；未被过滤器包装时按格式非法拒绝，不会按空 body 放行。
- 只支持同步 Servlet + Spring MVC（含 Spring Boot 3.x）；不支持 WebFlux，也不支持异步 Servlet 读取。
- 目标请求体是 JSON 或文本。`application/x-www-form-urlencoded` 的表单参数会混入
  `getParameterMap()`，与调用方的 query 规范化不一致，不支持。
- 自定义 Filter 若在链路中替换请求且没有保留属性，会导致拿不到缓存 body；拦截器会按格式非法拒绝，不会放行。
- 解密只保护 body 机密性；身份与完整性由签名保证，传输层仍应使用 HTTPS。
