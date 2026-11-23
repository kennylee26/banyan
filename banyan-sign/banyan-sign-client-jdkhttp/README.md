# banyan-sign-client-jdkhttp

JDK HttpClient 适配器：传入已构造的 `HttpRequest` 与 body，返回新的已签名请求。

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

- body 必须作为字符串单独传入，因为 `BodyPublisher` 不暴露原始内容；空 body 传 `null`
- 原请求的 method / URI / version / timeout / header 会被保留，body 换成密文
- 五个签名 header 会覆盖同名原 header
- 本适配器不包装 client：JDK HttpClient 默认不跟随重定向，保持默认即可；不要额外配置重试
