# banyan-commons

纯 JDK 的静态工具与可插拔接口集合：`error`（异常 + 消息资源）、`id`（ID 契约）、
`util`（字符串 / 反射 / 集合 / 编码 / 脱敏）、`crypto`（签名与防重放）。
Java package 的职责说明仍在各自的 `package-info.java`，本文只写使用方式与容易踩的坑。

模块**零 Spring、零第三方依赖**，只依赖 JDK 17。

## 签名与防重放（`crypto` 包）

用于对外 OpenAPI 的出站签名与入站验签。它只处理**纯数据**，不依赖任何 HTTP /
Spring 类型；HTTP 层由使用方自己接。

### 算法与密钥

- HMAC-SHA256（`javax.crypto.Mac`），签名串带 `alg` 标识
- 密钥由使用方构造：`new SignKey("app1", "secret")`，支持多个 `keyId` 并存轮换
- 库不读配置中心、环境变量或 Spring 容器；密钥怎么存完全由使用方决定
- 本期不兼容 MD5、不做国密、不做算法协商

### 规范化规则（固定，改动即破坏互操作性）

| 顺序 | 内容 |
|---|---|
| 1 | 版本标记 `BANYAN-SIGN-V1` |
| 2 | `method` |
| 3 | `path` |
| 4 | 查询串：key 升序、同名值字典序、空值跳过、UTF-8 URL 编码**一次**、排除 `sign` |
| 5 | `bodyHash`：请求体的 SHA-256（body 为 null 时按空串） |
| 6 | `timestamp`（毫秒） |
| 7 | `nonce` |
| 8 | `keyId` |
| 9 | `alg` |

各部分用换行符连接。可以用 `Signs.canonicalize(request)` 单独取出来调试。

### 出站签名

```java
SignKey key = new SignKey("app1", "secret");
SignRequest request = SignRequest.outbound(
        "POST", "/openapi/orders",
        Map.of("page", List.of("1")),
        "{\"id\":1}");

Map<String, String> params = Signs.signOutbound(request, key);
// params: alg / keyId / timestamp / nonce / sign
// 由使用方把这五个参数附加到 HTTP 请求的 header 或 query 上；
// 它们在规范化时会被排除，放哪里都不影响签名
```

`timestamp` 为毫秒，`nonce` 用 `SecureRandom` 生成、每次不同。

### 入站验签

```java
ReplayGuard guard = new ReplayGuard(
        new HmacSha256SignAlgorithm(),
        new InMemoryNonceStore());          // 单实例默认；多实例需替换 NonceStore

SignRequest inbound = new SignRequest(
        "POST", "/openapi/orders",
        Map.of("page", List.of("1")),
        body,
        Signs.parseTimestamp(headers.get("timestamp")),
        headers.get("nonce"),
        headers.get("keyId"),
        headers.get("alg"),
        headers.get("sign"));

VerifyResult result = guard.verifyInbound(inbound, Map.of("app1", key));
if (result != VerifyResult.SUCCESS) {
    // 统一失败：不区分签名错、过期、重放、未知 keyId
}
```

- 时间窗口默认 **5 分钟**（毫秒，可配）
- `nonce` 必填并参与签名；默认 `InMemoryNonceStore` 用 `ConcurrentHashMap` + 过期清理
- 验签用 `MessageDigest.isEqual` 常量时间比较
- 所有失败统一 `INVALID`，不抛异常、不泄漏原因；格式非法才抛 `IllegalArgumentException`
- 校验顺序：keyId/alg 查找 → 时间窗口 → 签名 → nonce 登记。过期请求不消耗签名计算与存储，
  坏签名不消耗 nonce；时间差达到窗口即视为过期

### 多实例部署

`InMemoryNonceStore` 只在单实例内去重。多实例（或需要重启不丢）时实现
`NonceStore` 接口并传入，例如用 Redis：

```java
NonceStore store = (key, ttlMillis) ->
        redis.opsForValue().setIfAbsent(key, "1", Duration.ofMillis(ttlMillis));
ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(), store);
```

库本身不引入 Redis 或任何中间件。

### 在 HTTP Filter 里组装（示范）

库不提供 Filter，下面只是使用方侧的接线示例：

```java
public class OpenApiSignFilter implements Filter {

    private final ReplayGuard guard = new ReplayGuard(
            new HmacSha256SignAlgorithm(), new InMemoryNonceStore());
    private final Map<String, SignKey> keys = Map.of("app1", new SignKey("app1", "secret"));

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String body = readBody(request);
        SignRequest signRequest = new SignRequest(
                request.getMethod(),
                request.getRequestURI(),
                toMultiMap(request.getParameterMap()),
                body,
                Signs.parseTimestamp(header(request, "timestamp")),
                header(request, "nonce"),
                header(request, "keyId"),
                header(request, "alg"),
                header(request, "sign"));

        if (guard.verifyInbound(signRequest, keys) != VerifyResult.SUCCESS) {
            ((HttpServletResponse) resp).sendError(401);
            return;
        }
        chain.doFilter(req, resp);
    }
}
```

注意：body 只能读一次，若后续业务还要读，请用可重复读的 wrapper。

## 安全边界

- 日志与异常不输出密钥、签名原始串、规范化串或 body
- `SignKey.toString()` 把 secret 显示为 `<redacted>`；`SignRequest.toString()` 不输出 body 与签名
- 库本身不写日志（commons 无日志依赖），失败原因需要排查时由使用方在自己的层记录

## 已知限制

- 本期只有 HMAC-SHA256，不做算法协商与国密；`alg` 标识为将来预留
- 签名不提供机密性，传输请用 HTTPS；AES/SM4 等加密能力不在本期
- 防重放依赖 `NonceStore`，默认内存实现只保证单实例
- 未签名的请求（缺 timestamp / nonce / keyId / alg）调用规范化会抛参数异常

## 模块结构

| 包 | 内容 |
|---|---|
| `error` | `BanyanException`、`ErrorCode`、`Messages`、消息解析 SPI |
| `id` | `IdProvider` 契约与 UUID 默认实现 |
| `util` | `Strings` / `Beans` / `CollUtils` / `Base64s` / `Hexs` / `Masks` |
| `crypto` | `Signs` / `ReplayGuard` / `SignAlgorithm` / `NonceStore` / `SignKey` / `SignRequest` / `VerifyResult` |

`json` / `http` / `system` 包不在本模块：JSON 能力在 `banyan-commons-spring` 的 `JsonCodec`
（依赖 Jackson，`provided + optional`），HTTP 客户端与系统信息按需直接使用 JDK。
