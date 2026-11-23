# banyan-sign-client-resttemplate

Spring RestTemplate 适配器：通过请求拦截器自动加密 body、计算签名并写入五个 header。

## 挂到已有模板（推荐）

```java
RestTemplate rest = new RestTemplate();
SigningRestTemplates.attach(rest, signer);   // 重复调用不会重复添加

ResponseEntity<String> response = rest.exchange(baseUrl + "/openapi/orders",
        HttpMethod.POST, new HttpEntity<>("{\"id\":1}", headers), String.class);
```

## 一行创建

```java
RestTemplate rest = SigningRestTemplates.create(signer);
```

`create` 使用 JDK HttpClient 并设置 `Redirect.NEVER`。

## 手动挂载

```java
restTemplate.getInterceptors().add(new SigningClientHttpRequestInterceptor(signer));
```

## 限制

- 拦截器直接拿到 Spring 传入的 `byte[]` body，无需额外配置缓冲工厂
- body 一律按 UTF-8 处理，不读取 `Content-Type` 的 charset
- multipart 请求不受支持，会明确抛出异常
- JDK HttpClient 默认不重试；不要额外配置重试处理器
- 已有 `RestTemplate` 时优先用 `attach`，避免自己拼拦截器
