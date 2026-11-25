# banyan-commons

banyan 的纯 JDK 基础工具箱。只依赖 Java 17，**不依赖 Spring，也没有第三方依赖**。

## 一、模块能力

- **统一异常与错误码**：引入后可用 `BanyanException` + `ErrorCode` 抛出带错误码的异常，消息由 `Messages` 资源束按中英文解析，不用各模块自己拼文案。
- **可替换的 ID 生成**：默认 `UuidIdProvider` 直接可用；换成雪花算法或数据库序列只需实现 `IdProvider`，调用侧不改。
- **常用 JDK 工具开箱即用**：字符串、集合、反射、Base64、十六进制、脱敏等静态工具随包提供，不引入任何第三方 jar。
- **OpenAPI 签名与防重放**：`Signs` / `ReplayGuard` 提供签名、验签、AES-GCM 载荷加解密与 nonce 防重放，纯 JDK 实现，不绑定 Servlet / Spring MVC / WebFlux 或任何 HTTP 客户端。
- **header 传播与日志值公共契约**：`HeaderContextHolder` 与 `LogSanitizer` 等纯 JDK 契约可供各出站客户端、日志能力共用，本模块不注册 bean、不做自动装配。

适合需要一组纯 JDK 基础工具、又不想被 Spring 或第三方依赖绑住的项目；若你需要 JSON 编解码、自动装配的日志输出或完整的 HTTP 客户端能力，引入前先确认这些由 `banyan-commons-spring` 等其它模块提供，本模块不含。各能力的源码位置见文末「五、能力 → 代码位置」。

## 二、模块里有什么

| 包 | 能解决什么 | 详细文档 |
|---|---|---|
| `error` | 统一异常、错误码和中英文消息 | [错误与消息](guides/error.md) |
| `id` | 可替换的 ID 生成器，默认 UUID 实现 | [ID 生成](guides/id.md) |
| `util` | 字符串、集合、反射、Base64、十六进制、脱敏 | [常用工具](guides/util.md) |
| `crypto` | OpenAPI 签名、验签和防重放 | [签名与防重放](guides/crypto.md) |

本文只负责整个模块的概览与导航；各能力的具体用法放在 `guides/` 下，
Java package 的职责边界仍由各自的 `package-info.java` 说明。

## 三、快速选择

- 想给 OpenAPI 请求签名或验签 → 看 [crypto 指南](guides/crypto.md)
- 想处理异常或读取错误码消息 → 看 [error 指南](guides/error.md)
- 想生成 ID → 看 [id 指南](guides/id.md)
- 想用字符串、编码、脱敏等小工具 → 看 [util 指南](guides/util.md)

## 四、与其它模块的边界

JSON 能力不在这里，而在 `banyan-commons-spring` 的 `JsonCodec`（依赖 Jackson，
`provided + optional`）。HTTP 客户端和系统信息按需直接使用 JDK。
`crypto` 包本身不绑定 Servlet、Spring MVC、WebFlux 或任何 HTTP 客户端。

本模块不提供配置：没有 `@ConfigurationProperties`、没有自动装配，也没有本模块 yml，全仓配置总表 `CONFIGURATION.md` 中无本模块条目。

## 五、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 统一异常与错误码 | [`error/`](src/main/java/cn/kennylee/banyan/commons/error/) | `BanyanException`、`ErrorCode`、`Messages`、`MessageResolver` |
| ID 生成契约与默认实现 | [`id/`](src/main/java/cn/kennylee/banyan/commons/id/) | `IdProvider`、`UuidIdProvider` |
| 字符串 / 编码 / 脱敏工具 | [`util/`](src/main/java/cn/kennylee/banyan/commons/util/) | `Strings`、`CollUtils`、`Beans`、`Base64s`、`Hexs`、`Masks` |
| OpenAPI 签名、验签与防重放 | [`crypto/`](src/main/java/cn/kennylee/banyan/commons/crypto/) | `Signs`、`RequestSigner`、`ReplayGuard`、`NonceStore`、`AesGcmPayloadCipher` |
| 出站 header 传播契约 | [`http/`](src/main/java/cn/kennylee/banyan/commons/http/) | `HeaderContext`、`HeaderContextHolder`、`HeaderForwardOptions`、`HeaderRules` |
| 日志脱敏与截断公共工具 | [`log/`](src/main/java/cn/kennylee/banyan/commons/log/) | `LogSanitizer`、`LoggingOptions` |
| 详细用法文档 | [`guides/`](guides/) | `error.md`、`id.md`、`util.md`、`crypto.md` |
