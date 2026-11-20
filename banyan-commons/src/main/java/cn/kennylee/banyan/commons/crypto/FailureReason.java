package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;

/**
 * 验签失败原因：只用于日志与联调，默认不得直接返回给外部调用方。
 *
 * <p>每个原因自带对应的 {@link ErrorCode}，接入层需要具体文案时直接取用，
 * 不再维护第二份映射。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum FailureReason {

    /** 缺少签名字段或时间戳无法解析。 */
    MALFORMED(ErrorCode.OPENAPI_SIGN_MALFORMED),

    /** keyId 未配置。 */
    UNKNOWN_KEY(ErrorCode.OPENAPI_SIGN_UNKNOWN_KEY),

    /** 请求携带的算法标识与提供方不一致。 */
    ALG_MISMATCH(ErrorCode.OPENAPI_SIGN_ALG_MISMATCH),

    /** 时间戳超出允许窗口。 */
    EXPIRED(ErrorCode.OPENAPI_SIGN_EXPIRED),

    /** nonce 已在窗口内使用过。 */
    REPLAY(ErrorCode.OPENAPI_SIGN_REPLAY),

    /** 重算签名与请求签名不一致。 */
    SIGNATURE_MISMATCH(ErrorCode.OPENAPI_SIGN_SIGNATURE_MISMATCH),

    /** 请求体解密失败；由接入层产生，验签入口不会返回。 */
    DECRYPT_FAILED(ErrorCode.PAYLOAD_DECRYPT_FAILED);

    /** 对应的错误码。 */
    private final ErrorCode errorCode;

    FailureReason(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 获取对应的错误码。
     *
     * @return 错误码
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
