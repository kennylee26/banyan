package cn.kennylee.banyan.commons.error;

/**
 * banyan 统一异常。携带错误码与参数，消息按当前语言动态解析。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class BanyanException extends RuntimeException {

    /** 错误码。 */
    private final ErrorCode errorCode;

    /** 消息占位符参数。 */
    private final transient Object[] args;

    /**
     * 构造实例。
     *
     * @param errorCode 错误码
     * @param args      消息占位符参数
     */
    public BanyanException(ErrorCode errorCode, Object... args) {
        this(errorCode, null, args);
    }

    /**
     * 构造实例。
     *
     * @param errorCode 错误码
     * @param cause     原始异常，可为 null
     * @param args      消息占位符参数
     */
    public BanyanException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.messageKey(), cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return Messages.get(errorCode, args);
    }
}
