# banyan-sign

OpenAPI 签名模块族：服务端验签 starter 与第三方 HTTP 客户端自动签名工具。

## 一、模块能力

- **服务端入站保护**：`banyan-sign-openapi` 用注解驱动验签与解密，失败统一 401，可替换密钥、去重、加密与签名实现。
- **客户端自动签名**：`banyan-sign-client` 一次调用完成「加密 → 签名 → 生成 5 个 header」，协议内核零 banyan 依赖，可单独发给第三方。
- **多 HTTP 客户端适配**：HttpClient 5 / RestTemplate / JDK HttpClient 三种适配器，按项目技术栈选用。
- **安全默认值**：默认加密模式，任何日志级别都不打印 secret、明文 body 与完整签名；适配器默认不跟随重定向、不自动重试。

适合需要给 OpenAPI 接口做签名/加密、且调用方可能不在本技术栈内的项目。调用方与服务端必须约定同一组 `keyId` / `secret` / header 名；只需简单鉴权、无需加密与防重放的场景，引入前先评估是否过重。各模块源码位置见文末「三、能力 → 代码位置」。

## 二、模块清单

| 模块 | 职责 | 依赖方向 |
|---|---|---|
| `banyan-sign-openapi` | 服务端：`@OpenApiSigned`、验签、解密、统一 401 | → `banyan-commons` |
| `banyan-sign-client` | 客户端协议内核：规范化、AES-256-GCM、HMAC-SHA256、header 组装 | 零 banyan 依赖；仅 `slf4j-api` |
| `banyan-sign-client-httpclient5` | HttpClient 5 适配器：请求拦截 + 构建助手 | → `banyan-sign-client` + `httpclient5` |
| `banyan-sign-client-resttemplate` | RestTemplate 适配器：请求拦截 + 构建/挂载助手 | → `banyan-sign-client` + `spring-web` |
| `banyan-sign-client-jdkhttp` | JDK HttpClient 适配器：传入请求 + body，返回已签名请求 | → `banyan-sign-client`，无额外依赖 |

调用方与服务端必须约定同一组 `keyId` / `secret` / header 名；协议细节见
[`banyan-sign-client/THIRD-PARTY-GUIDE.md`](banyan-sign-client/THIRD-PARTY-GUIDE.md)。

两个适配器都默认禁用重定向与自动重试；签名绑定 method / path / query / body，
重发请求需要调用方自己重新签名。

## 三、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 服务端验签 starter | [`banyan-sign-openapi/`](banyan-sign-openapi/) | `@OpenApiSigned`、`OpenApiSignInterceptor`、`RepeatableBodyFilter` |
| 客户端协议内核 | [`banyan-sign-client/`](banyan-sign-client/) | `OpenApiRequestSigner`、`SignedRequest`、`UnsignedRequest`、`SignHeaders` |
| HttpClient 5 适配器 | [`banyan-sign-client-httpclient5/`](banyan-sign-client-httpclient5/) | `SigningHttpRequests`、`SigningHttpClients` |
| RestTemplate 适配器 | [`banyan-sign-client-resttemplate/`](banyan-sign-client-resttemplate/) | `SigningRestTemplates`、`SigningClientHttpRequestInterceptor` |
| JDK HttpClient 适配器 | [`banyan-sign-client-jdkhttp/`](banyan-sign-client-jdkhttp/) | `SigningJdkHttpRequests` |
| 第三方对接说明 | [`THIRD-PARTY-GUIDE.md`](banyan-sign-client/THIRD-PARTY-GUIDE.md) | 协议细节与联调步骤 |
| 服务端配置参考 | [`banyan-sign-openapi.yml`](banyan-sign-openapi/banyan-sign-openapi.yml) | 全部 `banyan.openapi-sign.*` 键与默认值 |
