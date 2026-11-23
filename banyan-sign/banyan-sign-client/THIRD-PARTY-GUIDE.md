# banyan OpenAPI 加密与签名对接指南（第三方版）

本文面向**不引入任何 banyan 依赖**的调用方，只使用 JDK 与 Apache HttpClient 5。
按本文实现即可生成提供方能够验签、解密的请求。

## 1. 双方约定

| 项目 | 值 |
|---|---|
| keyId | 由提供方分配，例如 `app1` |
| secret | 双方线下交换，不通过网络发送 |
| 签名算法 | `HmacSHA256` |
| 加密算法 | `AES-256-GCM` |
| 时间窗口 | 默认 5 分钟，时间戳单位为毫秒 |
| Content-Type | `application/json` |

## 2. 一次请求要做什么

1. 准备明文 JSON。
2. 加密 body，得到 `wireBody`；空 body 跳过加密。
3. 对 `wireBody` 的 UTF-8 字节做 SHA-256，得到 `bodyHash`（小写十六进制）。
4. 按固定格式拼出规范化串 `canonical`。
5. 用 `HMAC-SHA256(canonical, secret)` 计算签名（小写十六进制）。
6. 发送原始 method / path / query + `wireBody`，并附加五个 header。

提供方顺序相反：读 header → 校验时间窗口 → 对**收到的 body** 重算签名 → 去重 nonce → 解密。

### 2.1 重定向与重试

签名绑定 method / path / body，重定向或自动重试后原签名必然失效：

- Apache HttpClient：关闭自动重定向与自动重试。
- JDK HttpClient：默认不跟随重定向，保持默认即可；不要额外配置重试。
- 需要重发请求时，重新生成 timestamp / nonce 并重新签名，不要复用旧 header。

## 3. 加密：AES-256-GCM

```
aesKey     = SHA-256(UTF8("banyan-aes") ‖ UTF8(secret))   // 32 字节
iv         = 随机 12 字节
ciphertext = AES-256-GCM(aesKey, iv, UTF8(明文))          // 含 16 字节 tag
wireBody   = Base64(iv ‖ ciphertext)                       // 标准 Base64，单行
```

- 加密的只有 **body**；method、path、query 不加密。
- 空 body 不加密，直接发送空 body。
- 解密：Base64 解码后，前 12 字节是 IV，其余是 `ciphertext + tag`；
  用同一把 `aesKey` 和 IV 调 `AES/GCM/NoPadding` 解密。

## 4. 签名：HMAC-SHA256

### 4.1 规范化串

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

规则：

- `path`：提供方验签时用的是「去掉 context-path 后」的路径。带 `/api` 前缀部署时，
  调用方签名前也要剥离 `/api`。
- `query`：参数名升序；同名参数按值字典序；空值跳过；key 与 value 各做一次
  UTF-8 URL 编码；排除 `alg` / `keyId` / `timestamp` / `nonce` / `sign`。
- `bodyHash`：对**实际发送的 body**（加密模式下是密文）做 SHA-256，输出小写十六进制；
  没有 body 时对空串计算。
- `timestamp`：当前毫秒时间戳。
- `nonce`：每次请求都不同的随机串，建议 16 字节随机数转十六进制。
- 签名：`HMAC-SHA256(canonical, UTF8(secret))`，输出小写十六进制。

### 4.2 五个 header

| 参数 | Header 默认名 | 值 | 说明 |
|---|---|---|---|
| alg | `X-Banyan-Alg` | `HmacSHA256` | 签名算法 |
| keyId | `X-Banyan-KeyId` | 提供方分配的 keyId | 用于选密钥 |
| timestamp | `X-Banyan-Timestamp` | 毫秒时间戳 | 时间窗口校验 |
| nonce | `X-Banyan-Nonce` | 每次不同的随机串 | 防重放 |
| sign | `X-Banyan-Sign` | HMAC 签名 | 小写十六进制 |

如果提供方改名，以对方给出的五个名字为准，值不变。加密与解密**不额外增加 header**，
`Content-Type` 仍是 `application/json`。

## 5. 可直接拷贝的纯 JDK 工具类

不依赖 banyan，也不需要第三方库；复制到自己的项目即可。

```java
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class BanyanOpenApiSigner {

    record SignedRequest(Map<String, String> headers, String body) {
    }

    private static final String VERSION = "BANYAN-SIGN-V1";
    private static final String ALG = "HmacSHA256";
    private static final String KEY_LABEL = "banyan-aes";
    private static final Set<String> PROTOCOL_PARAMS =
            Set.of("alg", "keyId", "timestamp", "nonce", "sign");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String keyId;
    private final String secret;
    private final boolean encrypted;
    private final String algHeader;
    private final String keyIdHeader;
    private final String timestampHeader;
    private final String nonceHeader;
    private final String signHeader;

    BanyanOpenApiSigner(String keyId, String secret) {
        this(keyId, secret, true, "X-Banyan-Alg", "X-Banyan-KeyId",
                "X-Banyan-Timestamp", "X-Banyan-Nonce", "X-Banyan-Sign");
    }

    BanyanOpenApiSigner(String keyId, String secret, String algHeader, String keyIdHeader,
                        String timestampHeader, String nonceHeader, String signHeader) {
        this(keyId, secret, true, algHeader, keyIdHeader, timestampHeader,
                nonceHeader, signHeader);
    }

    private BanyanOpenApiSigner(String keyId, String secret, boolean encrypted,
                                String algHeader, String keyIdHeader, String timestampHeader,
                                String nonceHeader, String signHeader) {
        this.keyId = keyId;
        this.secret = secret;
        this.encrypted = encrypted;
        this.algHeader = algHeader;
        this.keyIdHeader = keyIdHeader;
        this.timestampHeader = timestampHeader;
        this.nonceHeader = nonceHeader;
        this.signHeader = signHeader;
    }

    /** 明文模式：只签名原文，不加密。 */
    BanyanOpenApiSigner plainBody() {
        return new BanyanOpenApiSigner(keyId, secret, false, algHeader, keyIdHeader,
                timestampHeader, nonceHeader, signHeader);
    }

    SignedRequest sign(String method, String path,
                       Map<String, List<String>> query, String plaintextBody) {
        String body = encrypted ? encrypt(plaintextBody) : plaintextBody;
        String normalizedPath = path == null || path.isEmpty() ? "/" : path;
        long timestamp = System.currentTimeMillis();
        String nonce = newNonce();
        String canonical = canonicalize(method, normalizedPath, query, body, timestamp, nonce);
        String signature = hmac(canonical, secret);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(algHeader, ALG);
        headers.put(keyIdHeader, keyId);
        headers.put(timestampHeader, Long.toString(timestamp));
        headers.put(nonceHeader, nonce);
        headers.put(signHeader, signature);
        return new SignedRequest(Collections.unmodifiableMap(headers), body);
    }

    /** 从 URL 的 rawQuery 解析参数；没有 query 返回空 Map。 */
    static Map<String, List<String>> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> params = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            String name = decode(index < 0 ? pair : pair.substring(0, index));
            if (name.isBlank()) {
                continue;
            }
            String value = index < 0 ? "" : decode(pair.substring(index + 1));
            params.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }
        return params;
    }

    private String canonicalize(String method, String path, Map<String, List<String>> query,
                                String body, long timestamp, String nonce) {
        return VERSION + '\n' + method + '\n' + path + '\n' + queryString(query) + '\n'
                + sha256(body) + '\n' + timestamp + '\n' + nonce + '\n' + keyId + '\n' + ALG;
    }

    private static String queryString(Map<String, List<String>> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        new TreeMap<>(query).forEach((key, values) -> {
            if (PROTOCOL_PARAMS.contains(key) || values == null) {
                return;
            }
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .sorted()
                    .forEach(value -> pairs.add(encode(key) + '=' + encode(value)));
        });
        return String.join("&", pairs);
    }

    private String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        byte[] key = sha256Bytes(KEY_LABEL, secret);
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("AES-256-GCM 加密失败", e);
        }
    }

    private static String hmac(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String value = text == null ? "" : text;
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    private static byte[] sha256Bytes(String label, String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(label.getBytes(StandardCharsets.UTF_8));
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    private static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
```

## 6. 示例一：Apache HttpClient 5

```java
String url = "https://provider.example.com";
String method = "POST";
String path = "/openapi/orders";
String rawQuery = "page=1";
String plaintext = "{\"id\":1}";

BanyanOpenApiSigner signer = new BanyanOpenApiSigner("app1", secret);
BanyanOpenApiSigner.SignedRequest signed = signer.sign(
        method, path, BanyanOpenApiSigner.parseQuery(rawQuery), plaintext);

CloseableHttpClient httpClient = HttpClients.createDefault(); // 你已有的 client
HttpPost post = new HttpPost(url + path + (rawQuery.isEmpty() ? "" : "?" + rawQuery));
post.setEntity(new StringEntity(signed.body(), ContentType.APPLICATION_JSON));
signed.headers().forEach(post::setHeader);

try (CloseableHttpResponse response = httpClient.execute(post)) {
    System.out.println(response.getCode());
}
```

提供方关闭加密时，用明文模式；body 不再加密，签名对象仍是实际发送的 body：

```java
BanyanOpenApiSigner signer = new BanyanOpenApiSigner("app1", secret).plainBody();
```

自定义 header 名时，构造 signer 时传入五个名字：

```java
BanyanOpenApiSigner signer = new BanyanOpenApiSigner(
        "app1", secret,
        "X-Acme-Alg", "X-Acme-KeyId", "X-Acme-Timestamp", "X-Acme-Nonce", "X-Acme-Sign");
```

## 7. 示例二：JDK HttpClient

```java
String url = "https://provider.example.com";
String method = "POST";
String path = "/openapi/orders";
String rawQuery = "page=1";
String plaintext = "{\"id\":1}";

BanyanOpenApiSigner signer = new BanyanOpenApiSigner("app1", secret);
BanyanOpenApiSigner.SignedRequest signed = signer.sign(
        method, path, BanyanOpenApiSigner.parseQuery(rawQuery), plaintext);

HttpClient httpClient = HttpClient.newHttpClient(); // 你已有的 client
HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create(url + path + (rawQuery.isEmpty() ? "" : "?" + rawQuery)))
        .method(method, HttpRequest.BodyPublishers.ofString(
                signed.body() == null ? "" : signed.body(), StandardCharsets.UTF_8))
        .header("Content-Type", "application/json");
signed.headers().forEach(builder::setHeader);

HttpResponse<String> response = httpClient.send(
        builder.build(), HttpResponse.BodyHandlers.ofString());
```

## 8. 提供方如何解密

1. 从五个 header 读取 `keyId`、`timestamp`、`nonce`、`alg`、`sign`。
2. 按第 4 节用**收到的 body** 重算规范化串与签名，比较 `sign`。
3. 用 `keyId` 找到对应的 `secret`。
4. 按第 3 节解密 body，得到明文。

任何一步失败都返回统一错误，默认不向调用方透露具体原因。

## 9. 非 Java 实现检查清单

- [ ] SHA-256 与 HMAC-SHA256 输出小写十六进制
- [ ] AES-GCM 使用 12 字节 IV、16 字节 tag，Base64 拼接顺序是 IV 在前
- [ ] 签名对象是实际发送的 body（加密模式下是密文），不是明文
- [ ] 时间戳是毫秒，不是秒
- [ ] nonce 每次请求都变
- [ ] query 编码只做一次，且排除五个协议参数
- [ ] path 已剥离提供方的 context-path
- [ ] 五个 header 名与提供方一致
- [ ] 加密开关与提供方一致

## 10. 常见问题

| 现象 | 排查方向 |
|---|---|
| 一直签名不匹配 | 签名对象是否用了明文；query 是否编码了两次；path 是否带上 context-path |
| 加密后解密失败 | Base64 是否单行；IV 是否放在最前；secret 是否一致 |
| 偶发过期 | 双方时钟偏差；时间戳误用秒 |
| 重放被拒 | nonce 是否每次重新生成 |
| 请求体被截断 | 替换 body 后是否同步更新 Content-Length |
| 405 / 404 | path 与 HTTP method 是否和签名时一致 |
