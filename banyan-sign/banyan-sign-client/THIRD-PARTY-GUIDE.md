# banyan OpenAPI 签名对接指南（第三方版）

本文给调用方使用，**不要求引入任何 banyan 组件**。可以选择：

1. 直接拷贝本模块源码（推荐，零传递依赖）；
2. 使用 `banyan-sign-client` 与对应 HTTP 适配器 jar；
3. 按第 2 节协议用任意语言手工实现。

算法与字段顺序一旦约定就不要自行调整。

## 1. 双方约定

| 项目 | 值 |
|---|---|
| keyId | 由提供方分配，例如 `app1` |
| secret | 双方线下交换，不通过网络发送 |
| 签名算法 | `HmacSHA256` |
| 加密算法 | `AES-256-GCM` |
| 时间窗口 | 默认 5 分钟，时间戳单位为毫秒 |
| Content-Type | `application/json` |
| 签名 header | `X-Banyan-Alg` / `X-Banyan-KeyId` / `X-Banyan-Timestamp` / `X-Banyan-Nonce` / `X-Banyan-Sign` |

提供方可以整体改名五个 header；以对方给出的名字为准。

## 2. 协议

### 2.1 加密 body（加密模式）

```
aesKey     = SHA-256(UTF8("banyan-aes") ‖ UTF8(secret))   // 32 字节
iv         = 随机 12 字节
ciphertext = AES-256-GCM(aesKey, iv, UTF8(明文))          // 含 16 字节 tag
wireBody   = Base64(iv ‖ ciphertext)                       // 标准 Base64，单行
```

- body 为空时跳过加密，直接发送空 body
- 明文模式下不加密，直接发送原文；是否加密由双方约定

### 2.2 规范化串

按固定顺序拼接，每段之间用 `\n`：

```
BANYAN-SIGN-V1
{HTTP method，大写}
{path，例如 /openapi/orders}
{query 串}
{bodyHash}
{timestamp，毫秒}
{nonce}
{keyId}
HmacSHA256
```

- `path`：提供方使用的是「去掉 context-path 后」的路径；带 `/api` 前缀部署时，
  调用方也要在签名前剥离 `/api`
- `query`：参数名升序；同名参数按值字典序；空值跳过；key 与 value 各做一次
  UTF-8 URL 编码；排除 `alg` / `keyId` / `timestamp` / `nonce` / `sign`
- `bodyHash`：对**实际发送的 body**（加密模式下是密文）做 SHA-256，输出小写十六进制；
  没有 body 时对空串计算
- `nonce`：每次请求都不同的随机串，建议 16 字节随机数转十六进制

签名：`HMAC-SHA256(canonical, UTF8(secret))`，输出小写十六进制。

### 2.3 发送

业务 method / path / query 照常发送，body 用 `wireBody`，再加五个 header：

| Header | 值 |
|---|---|
| `X-Banyan-Alg` | `HmacSHA256` |
| `X-Banyan-KeyId` | keyId |
| `X-Banyan-Timestamp` | 毫秒时间戳 |
| `X-Banyan-Nonce` | nonce |
| `X-Banyan-Sign` | 签名 |

处理顺序：先加密 body，再对密文签名（encrypt-then-sign）。

## 3. Java：Apache HttpClient 5（原地签名）

```java
OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .build();

HttpPost post = new HttpPost("https://provider.example.com/openapi/orders?page=1");
post.setEntity(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));

SigningHttpRequests.sign(signer, post);   // 原地加密 + 写 5 个 header

try (CloseableHttpResponse response = myHttpClient.execute(post)) {
    System.out.println(response.getCode());
}
```

- `myHttpClient` 可以是任何已构造的 HttpClient；签名工具只改请求
- 想全自动时用 `SigningHttpClients.create(signer)` 或把 `SigningHttpRequestInterceptor` 注册到 client
- 只支持 `StringEntity` 与 `ByteArrayEntity`；流式、文件与 multipart 直接抛异常
- 自动拦截版本已禁用重定向与自动重试；原地签名由调用方的 client 自行决定

## 4. Java：JDK HttpClient

```java
HttpRequest unsigned = HttpRequest.newBuilder(
                URI.create("https://provider.example.com/openapi/orders?page=1"))
        .method("POST", HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", "application/json")
        .build();

HttpRequest signed = SigningJdkHttpRequests.sign(signer, unsigned, "{\"id\":1}");

HttpResponse<String> response = HttpClient.newHttpClient()
        .send(signed, HttpResponse.BodyHandlers.ofString());
```

- JDK HttpClient 没有请求拦截器 API，所以工具只转换请求，不包装 client
- body 必须作为字符串单独传入，因为 `BodyPublisher` 不暴露原始内容

## 5. Java：Spring RestTemplate

```java
OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .build();

RestTemplate rest = new RestTemplate();
SigningRestTemplates.attach(rest, signer);

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

ResponseEntity<String> response = rest.exchange(
        "https://provider.example.com/openapi/orders",
        HttpMethod.POST,
        new HttpEntity<>("{\"id\":1}", headers),
        String.class);
```

- `attach` 可以挂到任何已有 RestTemplate，重复调用不会重复添加
- 一行创建时用 `SigningRestTemplates.create(signer)`（使用 JDK HttpClient 并禁止跟随重定向）
- multipart 请求不支持

## 6. 常用配置

| 场景 | 写法 |
|---|---|
| 明文模式（提供方关闭加密） | `OpenApiRequestSigner.builder()...plainBody().build()` |
| context-path 部署 | `.basePath("/api")`，请求路径 `/api/openapi/orders` 会按 `/openapi/orders` 签名 |
| 自定义 header 名 | 见下方「自定义 header 名」 |
| 多调用方隔离 | 每个调用方各自创建 `OpenApiRequestSigner` 实例，不共享静态配置 |

### 自定义 header 名

调用方与提供方必须使用同一组名字。**加密与解密都不额外增加 header**：body 始终是
`application/json`，五个 header 只承载签名参数。

客户端：

```java
SignHeaders headers = new SignHeaders(
        "X-Acme-Alg",
        "X-Acme-KeyId",
        "X-Acme-Timestamp",
        "X-Acme-Nonce",
        "X-Acme-Sign");

OpenApiRequestSigner signer = OpenApiRequestSigner.builder()
        .keyId("app1")
        .secret(secret)
        .headers(headers)
        .build();
```

提供方 `application.yml`：

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

`SignHeaders.DEFAULT` 只是默认值常量，Builder 默认已经用它，通常不需要显式传入；
只有需要引用默认名字时才用。五个名字必须非空、互不重复（不区分大小写），且符合
HTTP header token 字符集。

## 7. 拷贝源码 vs 依赖 jar

- 只拷贝源码：复制 `cn/kennylee/banyan/openapi/sign/client` 包，以及需要的适配器包；
  运行时只需 JDK 17 + `slf4j-api`，适配器另需 `httpclient5` 或 `spring-web`
- 依赖 jar：`banyan-sign-client` 本身不依赖 banyan 其它模块；两个适配器分别引入
  `httpclient5` 与 `spring-web`

## 8. 非 Java 实现检查清单

- [ ] SHA-256 与 HMAC-SHA256 输出小写十六进制
- [ ] AES-GCM 使用 12 字节 IV、16 字节 tag，Base64 拼接顺序是 IV 在前
- [ ] 签名对象是实际发送的 body（加密模式下是密文），不是明文
- [ ] 时间戳是毫秒，不是秒
- [ ] nonce 每次请求都变
- [ ] query 编码只做一次，且排除五个协议参数
- [ ] path 已剥离提供方的 context-path
- [ ] 加密开关与提供方一致

## 9. 失败响应

默认固定 401：

```json
{"code":"openapi.sign.invalid","message":"签名校验失败"}
```

提供方开启 `reveal-reason` 时 `message` 会指出具体原因（过期、重放、签名不匹配等），
`code` 不变。联调完成后建议提供方关闭该开关。

## 10. 常见问题

| 现象 | 排查方向 |
|---|---|
| 一直签名不匹配 | 签名对象是否用了明文；query 是否编码了两次；path 是否带上 context-path |
| 加密后解密失败 | Base64 是否单行；IV 是否放在最前；secret 是否一致 |
| 偶发过期 | 双方时钟偏差；时间戳误用秒 |
| 重放被拒 | nonce 是否每次重新生成 |
| 请求体被截断 | 替换 body 后是否同步更新 Content-Length |
| 405 / 404 | path 与 HTTP method 是否和签名时一致 |
