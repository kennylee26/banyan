# error：错误码与消息

`error` 包提供统一异常、错误码和中英文消息资源：

- `ErrorCode`：集中维护各模块的错误码，对应消息资源中的 key。
- `Messages`：按错误码读取消息，支持占位符和语言切换。
- `BanyanException`：携带错误码与参数的领域异常。
- `MessageResolver` / `ResourceBundleMessageResolver`：消息解析扩展点与 JDK 默认实现。

```java
throw new BanyanException(ErrorCode.SIGN_REQUEST_INVALID, "timestamp");
```

消息文案集中维护在 `banyan-messages.properties` 与 `banyan-messages_en.properties`，
不要在业务代码里硬编码中文错误提示。

各包职责仍以 `package-info.java` 为准。
