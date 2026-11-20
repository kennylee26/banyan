package cn.kennylee.banyan.openapi.sign.web;

import cn.kennylee.banyan.commons.crypto.FailureReason;
import cn.kennylee.banyan.commons.crypto.PayloadCipher;
import cn.kennylee.banyan.commons.crypto.ReplayGuard;
import cn.kennylee.banyan.commons.crypto.SignHeaders;
import cn.kennylee.banyan.commons.crypto.SignKey;
import cn.kennylee.banyan.commons.crypto.UnsignedRequest;
import cn.kennylee.banyan.commons.crypto.VerifyOutcome;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.openapi.sign.annotation.OpenApiSigned;
import cn.kennylee.banyan.openapi.sign.autoconfigure.OpenApiSignProperties;
import cn.kennylee.banyan.openapi.sign.support.ErrorResponses;
import cn.kennylee.banyan.openapi.sign.support.SignKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 注解驱动的入站验签与解密：先验签、后解密，失败统一 401。
 *
 * <p>body 由 {@link RepeatableBodyFilter} 缓存；解密后的明文写回请求属性，
 * 由包装器在读取时替换。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class OpenApiSignInterceptor implements HandlerInterceptor {

    /** 日志字段中的换行，避免日志注入。 */
    private static final Pattern LINE_BREAK = Pattern.compile("[\\r\\n]");

    /** 配置。 */
    private final OpenApiSignProperties properties;

    /** 验签守卫。 */
    private final ReplayGuard guard;

    /** 密钥解析。 */
    private final SignKeyResolver keyResolver;

    /** header 名配置。 */
    private final SignHeaders headersConfig;

    /** 加密实现。 */
    private final PayloadCipher cipher;

    /**
     * 构造拦截器。
     *
     * @param properties    配置
     * @param guard         验签守卫
     * @param keyResolver   密钥解析
     * @param headersConfig header 名配置
     * @param cipher        加密实现
     */
    public OpenApiSignInterceptor(OpenApiSignProperties properties, ReplayGuard guard,
                                  SignKeyResolver keyResolver, SignHeaders headersConfig,
                                  PayloadCipher cipher) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.guard = Objects.requireNonNull(guard, "guard");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
        this.headersConfig = Objects.requireNonNull(headersConfig, "headersConfig");
        this.cipher = Objects.requireNonNull(cipher, "cipher");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod) || !isAnnotated(handlerMethod)) {
            return true;
        }

        String keyId = request.getHeader(headersConfig.keyId());
        SignKey key = Strings.isBlank(keyId) ? null : keyResolver.resolve(keyId);
        Map<String, SignKey> keys = key == null ? Map.of() : Map.of(key.keyId(), key);
        byte[] body = cachedBody(request);
        if (body == null) {
            reject(request, response, FailureReason.MALFORMED, keyId);
            return false;
        }
        String bodyText = new String(body, StandardCharsets.UTF_8);
        VerifyOutcome outcome = guard.verifyDetailed(
                UnsignedRequest.of(request.getMethod(), pathOf(request),
                        queryParams(request), bodyText),
                headers(request), keys);
        if (!outcome.isSuccess()) {
            reject(request, response, outcome.reason(), keyId);
            return false;
        }
        if (!properties.isEncrypted() || body.length == 0) {
            return true;
        }
        return decrypt(request, response, bodyText, key, keyId);
    }

    /** 验签通过后解密，并把明文写回请求属性。 */
    private boolean decrypt(HttpServletRequest request, HttpServletResponse response,
                            String bodyText, SignKey key, String keyId) throws IOException {
        try {
            String plaintext = cipher.decrypt(bodyText, key.secret());
            request.setAttribute(CachedBodyRequestWrapper.PLAIN_BODY_ATTRIBUTE,
                    plaintext.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (RuntimeException e) {
            reject(request, response, FailureReason.DECRYPT_FAILED, keyId);
            return false;
        }
    }

    /** 写日志并返回统一 401。 */
    private void reject(HttpServletRequest request, HttpServletResponse response,
                        FailureReason reason, String keyId) throws IOException {
        log.warn(Messages.get(ErrorCode.OPENAPI_SIGN_VERIFY_FAILED, reason, request.getMethod(),
                pathOf(request), safeKeyId(keyId)));
        ErrorResponses.writeUnauthorized(response, properties.getError().getCode(),
                messageFor(reason));
    }

    /** 响应文案：默认统一，开启后按原因返回。 */
    private String messageFor(FailureReason reason) {
        if (properties.isRevealReason()) {
            return Messages.get(reason.errorCode());
        }
        String configured = properties.getError().getMessage();
        return Strings.isNotBlank(configured) ? configured
                : Messages.get(ErrorCode.OPENAPI_SIGN_INVALID);
    }

    /** 是否符合注解要求：方法级或类级任一存在即可。 */
    private static boolean isAnnotated(HandlerMethod handlerMethod) {
        return handlerMethod.getMethodAnnotation(OpenApiSigned.class) != null
                || handlerMethod.getBeanType().isAnnotationPresent(OpenApiSigned.class);
    }

    /**
     * 从请求属性取缓存的原始 body。
     *
     * @return 缓存内容；请求未被 {@link RepeatableBodyFilter} 包装时返回 null
     */
    private static byte[] cachedBody(HttpServletRequest request) {
        Object cached = request.getAttribute(CachedBodyRequestWrapper.CACHED_BODY_ATTRIBUTE);
        return cached instanceof byte[] bytes ? bytes : null;
    }

    /**
     * 去掉 context-path，得到调用方签名时使用的 path。
     *
     * <p>Servlet 规范保证 contextPath 非 null，且请求 URI 以它为前缀；
     * 根上下文时为空串，截取长度 0 即原样返回。</p>
     */
    private static String pathOf(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return request.getRequestURI().substring(contextPath.length());
    }

    /** 读取五个签名 header。 */
    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>(5);
        headers.put(headersConfig.alg(), request.getHeader(headersConfig.alg()));
        headers.put(headersConfig.keyId(), request.getHeader(headersConfig.keyId()));
        headers.put(headersConfig.timestamp(), request.getHeader(headersConfig.timestamp()));
        headers.put(headersConfig.nonce(), request.getHeader(headersConfig.nonce()));
        headers.put(headersConfig.sign(), request.getHeader(headersConfig.sign()));
        return headers;
    }

    /** 把 Servlet 参数表转换为协议需要的结构。 */
    private static Map<String, List<String>> queryParams(HttpServletRequest request) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            List<String> nonNullValues = new ArrayList<>(values.length);
            for (String value : values) {
                if (value != null) {
                    nonNullValues.add(value);
                }
            }
            params.put(name, nonNullValues);
        });
        return params;
    }

    /** keyId 脱敏，避免日志注入；空值用占位符。 */
    private static String safeKeyId(String keyId) {
        return Strings.isBlank(keyId) ? "-" : LINE_BREAK.matcher(keyId).replaceAll("_");
    }
}
