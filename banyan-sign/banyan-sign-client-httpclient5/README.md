# banyan-sign-client-httpclient5

Apache HttpClient 5 适配器：在请求发出前自动加密 body、计算签名并写入五个 header。

```java
OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .build();

try (CloseableHttpClient client = SigningHttpClients.builder(signer).build()) {
    HttpPost post = new HttpPost(baseUrl + "/openapi/orders");
    post.setEntity(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));
    try (CloseableHttpResponse response = client.execute(post)) {
        // ...
    }
}
```

- `SigningHttpClients.builder(signer)` 已禁用重定向与自动重试，并注册 `SigningHttpRequestInterceptor`
- 只接受 `StringEntity` 与 `ByteArrayEntity`；流式、文件与 multipart 直接抛异常
- 替换 entity 后会同步更新 `Content-Length`，避免密文被截断
- 需要自己组装 client 时，把 `new SigningHttpRequestInterceptor(signer)` 注册为请求拦截器即可
