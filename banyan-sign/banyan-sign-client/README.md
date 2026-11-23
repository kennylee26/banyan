# banyan-sign-client

第三方 OpenAPI 签名客户端：一次调用完成「加密 → 签名 → 生成 header」，不依赖 banyan 其它模块。

- 协议内核只依赖 JDK 17 与 `slf4j-api`
- 默认加密模式（encrypt-then-sign），`plainBody()` 可切明文模式
- keyId / secret / header 名 / basePath / 模式均为实例级配置
- 日志按 SLF4J 级别输出，任何级别都不打印 secret、明文 body、完整签名

不引入 banyan 组件时，直接把 [`THIRD-PARTY-GUIDE.md`](THIRD-PARTY-GUIDE.md) 发给对方；
需要依赖 jar 时使用：

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-sign-client</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-sign-client-httpclient5</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 只用协议内核

```java
OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .build();

SignedRequest signed = signer.sign(new UnsignedRequest(
        "POST", "/openapi/orders", Map.of("page", List.of("1")), "{\"id\":1}"));

signed.headers(); // 五个签名 header
signed.body();    // 密文，直接作为请求体发送
```

## HTTP 客户端接入

| 客户端 | 推荐写法 |
|---|---|
| Apache HttpClient 5 | `SigningHttpRequests.sign(signer, request)` 原地签名，client 自己构造 |
| Spring RestTemplate | `SigningRestTemplates.attach(restTemplate, signer)` 挂到已有模板 |
| JDK HttpClient | `SigningJdkHttpRequests.sign(signer, request, body)` 返回已签名请求 |

想省事时，两个拦截器版本也可以一行创建：`SigningHttpClients.create(signer)`、
`SigningRestTemplates.create(signer)`。JDK HttpClient 没有拦截器 API，只能转换请求。

## 配置

| Builder 方法 | 默认 | 说明 |
|---|---|---|
| `keyId` / `secret` | 无 | 双方约定的密钥，必填 |
| `headers(SignHeaders)` | `X-Banyan-*` | 五个 header 名，须与提供方一致 |
| `basePath(String)` | 空 | 从请求路径剥离的前缀，例如 `/api`；不匹配直接失败 |
| `plainBody()` | 加密 | 切换到明文签名，对应服务端 `encrypted=false` |

自定义五个 header 名时，客户端与提供方必须一致：

```java
SignHeaders headers = new SignHeaders(
        "X-Acme-Alg", "X-Acme-KeyId", "X-Acme-Timestamp", "X-Acme-Nonce", "X-Acme-Sign");

OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .headers(headers)
        .build();
```

`SignHeaders.DEFAULT` 是默认值常量，Builder 默认已使用，通常不需要显式传入。
加密与解密不额外增加 header，五个 header 只承载签名参数。

## 限制

- body 按 UTF-8 文本处理，不解析 JSON；流式 / multipart body 会被适配器拒绝
- 不支持 WebFlux / `WebClient` / 异步 HttpClient
- 适配器不跟随重定向、不自动重试；需要重发时重新签名
