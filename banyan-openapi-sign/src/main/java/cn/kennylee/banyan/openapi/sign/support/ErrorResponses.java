package cn.kennylee.banyan.openapi.sign.support;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 统一失败响应：401 + {@code {"code":"...","message":"..."}}。
 *
 * <p>只写两个固定字段，手写 JSON 转义以避免引入额外依赖。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class ErrorResponses {

    /** 常用字符的短转义。 */
    private static final Map<Character, String> SHORT_ESCAPES = Map.of(
            '"', "\\\"",
            '\\', "\\\\",
            '\b', "\\b",
            '\f', "\\f",
            '\n', "\\n",
            '\r', "\\r",
            '\t', "\\t");

    private ErrorResponses() {
    }

    /**
     * 写出 401 错误响应。
     *
     * @param response 响应
     * @param code     错误码
     * @param message  错误文案
     * @throws IOException 写出失败
     */
    public static void writeUnauthorized(HttpServletResponse response, String code, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}");
        writer.flush();
    }

    /**
     * 转义 JSON 字符串内容。
     *
     * @param value 原值，可为 null
     * @return 转义结果
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String shortForm = SHORT_ESCAPES.get(c);
            if (shortForm != null) {
                escaped.append(shortForm);
            } else if (c < 0x20) {
                escaped.append(String.format("\\u%04x", (int) c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
