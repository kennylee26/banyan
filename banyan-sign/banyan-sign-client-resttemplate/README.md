# banyan-sign-client-resttemplate

Spring RestTemplate 适配器：通过请求拦截器自动加密 body、计算签名并写入五个 header。

```java
OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .build();

RestTemplate rest = SigningRestTemplates.create(signer);
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

ResponseEntity<String> response = rest.exchange(baseUrl + "/openapi/orders",
        HttpMethod.POST, new HttpEntity<>("{\"id\":1}", headers), String.class);
```

- `SigningRestTemplates.create(signer)` 使用 JDK HttpClient 并设置 `Redirect.NEVER`，已注册拦截器
- 拦截器直接拿到 Spring 传入的 `byte[]` body，无需额外配置缓冲工厂
- multipart 请求不受支持，会明确抛出异常
- JDK HttpClient 默认不重试；不要额外配置重试处理器
- 已有 `RestTemplate` 时，把 `new SigningClientHttpRequestInterceptor(signer)` 加入 `getInterceptors()` 即可
