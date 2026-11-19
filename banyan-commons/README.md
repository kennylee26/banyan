# banyan-commons

banyan 的纯 JDK 基础工具箱。只依赖 Java 17，**不依赖 Spring，也没有第三方依赖**。

## 模块里有什么

| 包 | 能解决什么 | 详细文档 |
|---|---|---|
| `error` | 统一异常、错误码和中英文消息 | [错误与消息](guides/error.md) |
| `id` | 可替换的 ID 生成器，默认 UUID 实现 | [ID 生成](guides/id.md) |
| `util` | 字符串、集合、反射、Base64、十六进制、脱敏 | [常用工具](guides/util.md) |
| `crypto` | OpenAPI 签名、验签和防重放 | [签名与防重放](guides/crypto.md) |

本文只负责整个模块的概览与导航；各能力的具体用法放在 `guides/` 下，
Java package 的职责边界仍由各自的 `package-info.java` 说明。

## 快速选择

- 想给 OpenAPI 请求签名或验签 → 看 [crypto 指南](guides/crypto.md)
- 想处理异常或读取错误码消息 → 看 [error 指南](guides/error.md)
- 想生成 ID → 看 [id 指南](guides/id.md)
- 想用字符串、编码、脱敏等小工具 → 看 [util 指南](guides/util.md)

## 与其它模块的边界

JSON 能力不在这里，而在 `banyan-commons-spring` 的 `JsonCodec`（依赖 Jackson，
`provided + optional`）。HTTP 客户端和系统信息按需直接使用 JDK。
`crypto` 包本身不绑定 Servlet、Spring MVC、WebFlux 或任何 HTTP 客户端。
