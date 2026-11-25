# banyan-sign-client-resttemplate

Spring RestTemplate 适配器：通过请求拦截器自动加密 body、计算签名并写入五个 header。

## 一、模块能力

- **挂到已有模板**：`SigningRestTemplates.attach(...)` 给已有 `RestTemplate` 加拦截器，重复调用不会重复添加。
- **一行创建**：`SigningRestTemplates.create(...)` 直接得到带签名的模板。
- **手动挂载**：也可直接把 `SigningClientHttpRequestInterceptor` 加进 `getInterceptors()`。
- **安全默认值**：`create` 设置 `Redirect.NEVER`；body 一律按 UTF-8 处理，multipart 明确拒绝。

适合用 Spring `RestTemplate` 调用 OpenAPI 接口的项目；签名逻辑在 [`banyan-sign-client`](../banyan-sign-client/README.md) 协议内核，本模块只做适配。已有 `RestTemplate` 时优先用 `attach`，避免自己拼拦截器。各能力的源码位置见文末「六、能力 → 代码位置」。

## 二、挂到已有模板（推荐）

```java
RestTemplate rest = new RestTemplate();
SigningRestTemplates.attach(rest, signer);   // 重复调用不会重复添加

ResponseEntity<String> response = rest.exchange(baseUrl + "/openapi/orders",
        HttpMethod.POST, new HttpEntity<>("{\"id\":1}", headers), String.class);
```

## 三、一行创建

```java
RestTemplate rest = SigningRestTemplates.create(signer);
```

`create` 使用 JDK HttpClient 并设置 `Redirect.NEVER`。

## 四、手动挂载

```java
restTemplate.getInterceptors().add(new SigningClientHttpRequestInterceptor(signer));
```

## 五、限制

- 拦截器直接拿到 Spring 传入的 `byte[]` body，无需额外配置缓冲工厂
- body 一律按 UTF-8 处理，不读取 `Content-Type` 的 charset
- multipart 请求不受支持，会明确抛出异常
- JDK HttpClient 默认不重试；不要额外配置重试处理器
- 已有 `RestTemplate` 时优先用 `attach`，避免自己拼拦截器

## 六、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 挂载 / 创建模板 | [`resttemplate/`](src/main/java/cn/kennylee/banyan/openapi/sign/client/resttemplate/) | `SigningRestTemplates` |
| 请求拦截器 | [`resttemplate/`](src/main/java/cn/kennylee/banyan/openapi/sign/client/resttemplate/) | `SigningClientHttpRequestInterceptor` |
| 协议内核 | 兄弟模块 [`banyan-sign-client/`](../banyan-sign-client/) | `OpenApiRequestSigner`、`SignHeaders` |

本模块**没有 java config**，不存在 `<artifactId>.yml` 与 `banyan.*` 配置项；全仓配置项总表见 [`../../CONFIGURATION.md`](../../CONFIGURATION.md)。
