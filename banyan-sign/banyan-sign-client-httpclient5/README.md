# banyan-sign-client-httpclient5

Apache HttpClient 5 适配器：在请求发出前自动加密 body、计算签名并写入五个 header。

## 一、模块能力

- **原地签名**：`SigningHttpRequests.sign(...)` 对自己构造的请求加密并写 5 个 header，client 仍由调用方掌控。
- **自动拦截**：`SigningHttpClients.create(...)` / `builder(...)` 得到带拦截器的 client，省掉每次手动签名。
- **安全默认值**：自动拦截的 client 已禁用重定向与自动重试；替换 entity 后同步更新 `Content-Length`，避免密文被截断。

适合用 Apache HttpClient 5 调用 OpenAPI 接口的项目；签名逻辑在 [`banyan-sign-client`](../banyan-sign-client/README.md) 协议内核，本模块只做适配。流式 / 文件 / multipart body 不受支持，引入前先确认请求体形态。各能力的源码位置见文末「五、能力 → 代码位置」。

## 二、原地签名（推荐）

自己构造请求和 client，把请求交给工具签名即可：

```java
HttpPost post = new HttpPost(baseUrl + "/openapi/orders");
post.setEntity(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));

SigningHttpRequests.sign(signer, post);   // 原地加密 + 写 5 个 header

try (CloseableHttpResponse response = myHttpClient.execute(post)) {
    // ...
}
```

## 三、自动拦截

想省掉每次手动调用时，可以用带拦截器的 client：

```java
// 一行创建
try (CloseableHttpClient client = SigningHttpClients.create(signer)) {
    // ...
}

// 或先定制 builder
CloseableHttpClient client = SigningHttpClients.builder(signer)
        .setConnectionManager(connectionManager)
        .build();
```

## 四、限制

- `SigningHttpRequests.sign` 与拦截器都只接受 `StringEntity` 与 `ByteArrayEntity`；
  流式、文件与 multipart entity 直接抛异常
- 替换 entity 后会同步更新 `Content-Length`，避免密文被截断
- 自动拦截的 client 已禁用重定向与自动重试；原地签名由调用方的 client 自行决定
- 同一个请求不要重复签名，否则会二次加密

## 五、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 原地签名 | [`httpclient5/`](src/main/java/cn/kennylee/banyan/openapi/sign/client/httpclient5/) | `SigningHttpRequests` |
| 自动拦截 client | [`httpclient5/`](src/main/java/cn/kennylee/banyan/openapi/sign/client/httpclient5/) | `SigningHttpClients`、`SigningHttpRequestInterceptor` |
| 协议内核 | 兄弟模块 [`banyan-sign-client/`](../banyan-sign-client/) | `OpenApiRequestSigner`、`SignHeaders` |

本模块**没有 java config**，不存在 `<artifactId>.yml` 与 `banyan.*` 配置项；全仓配置项总表见 [`../../CONFIGURATION.md`](../../CONFIGURATION.md)。
