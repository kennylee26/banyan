# banyan-sign

OpenAPI 签名模块族：服务端验签 starter 与第三方 HTTP 客户端自动签名工具。

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
