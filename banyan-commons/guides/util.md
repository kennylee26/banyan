# util：常用纯 JDK 工具

这些工具只依赖 JDK，不注册 bean、不需要 Spring。

| 类 | 常见用途 |
|---|---|
| `Strings` | 判空、驼峰与下划线转换 |
| `Beans` | 把 bean 的 getter 属性读取为 Map |
| `CollUtils` | 集合判空、分片 |
| `Base64s` | Base64 编解码 |
| `Hexs` | 字节数组与十六进制文本互转 |
| `Masks` | 手机号、邮箱等文本脱敏 |

## 示例

```java
Strings.isBlank("  ");                 // true
Strings.toUnderlineCase("userName");   // user_name

Base64s.encode("hello");               // aGVsbG8=
Hexs.encode(new byte[] {1, 15});       // 010f

Masks.maskMobile("13812348000");       // 脱敏后的手机号
```

具体方法签名与空值行为以对应类的 Javadoc 为准。
