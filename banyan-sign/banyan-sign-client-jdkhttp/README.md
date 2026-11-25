# banyan-sign-client-jdkhttp

JDK HttpClient 适配器：传入已构造的 `HttpRequest` 与 body，返回新的已签名请求。

## 一、模块能力

- **转换请求而非包装 client**：JDK `HttpClient` 没有拦截器 API、`HttpRequest` 也不可变，本模块只做「传入原请求 + body → 返回已签名新请求」。
- **保留原请求属性**：method / URI / version / timeout / header 都会保留，body 换成密文，5 个签名 header 覆盖同名原 header。
- **跟随 JDK 默认行为**：不跟随重定向、不自动重试，调用方自行 `send`；重发时需重新签名。

适合用 JDK 内置 `HttpClient` 调用 OpenAPI 接口、不想引入第三方 HTTP 依赖的项目；签名逻辑在 [`banyan-sign-client`](../banyan-sign-client/README.md) 协议内核。`BodyPublisher` 不暴露原始内容，body 必须作为字符串单独传入；流式 / multipart 场景不适用。各能力的源码位置见文末「四、能力 → 代码位置」。

## 二、用法

JDK 的 `HttpClient` 没有请求拦截器 API，`HttpRequest` 也不可变，所以这里不包装 client，
只转换请求。

```java
HttpRequest unsigned = HttpRequest.newBuilder(URI.create(url + "/openapi/orders?page=1"))
        .method("POST", HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", "application/json")
        .build();

HttpRequest signed = SigningJdkHttpRequests.sign(signer, unsigned, "{\"id\":1}");

HttpResponse<String> response = HttpClient.newHttpClient()
        .send(signed, HttpResponse.BodyHandlers.ofString());
```

## 三、限制

- body 必须作为字符串单独传入，因为 `BodyPublisher` 不暴露原始内容；空 body 传 `null`
- 原请求的 method / URI / version / timeout / header 会被保留，body 换成密文
- 五个签名 header 会覆盖同名原 header
- 本适配器不包装 client：JDK HttpClient 默认不跟随重定向，保持默认即可；不要额外配置重试

## 四、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 请求签名转换 | [`jdkhttp/`](src/main/java/cn/kennylee/banyan/openapi/sign/client/jdkhttp/) | `SigningJdkHttpRequests` |
| 协议内核 | 兄弟模块 [`banyan-sign-client/`](../banyan-sign-client/) | `OpenApiRequestSigner`、`SignHeaders` |

本模块**没有 java config**，不存在 `<artifactId>.yml` 与 `banyan.*` 配置项；全仓配置项总表见 [`../../CONFIGURATION.md`](../../CONFIGURATION.md)。
