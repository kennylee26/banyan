package cn.kennylee.banyan.commons.spring.log;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 出站请求日志配置：脱敏头、忽略头与 body 上限。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
public class LoggingOptions {

    /** 默认 body 打印上限。 */
    public static final int DEFAULT_MAX_BODY_LENGTH = 2048;

    /** 敏感值脱敏。 */
    private boolean maskSensitive = true;

    /** 需要脱敏的 header。 */
    private List<String> maskedHeaders = new ArrayList<>(
            List.of("Authorization", "Cookie", "Set-Cookie"));

    /** 完全不打印的 header。 */
    private List<String> ignoredHeaders = new ArrayList<>();

    /** body 打印上限。 */
    private int maxBodyLength = DEFAULT_MAX_BODY_LENGTH;
}
