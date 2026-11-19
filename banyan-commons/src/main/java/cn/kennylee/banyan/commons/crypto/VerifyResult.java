package cn.kennylee.banyan.commons.crypto;

/**
 * 入站验签结果：所有失败统一为 {@link #INVALID}，不对外区分具体原因。
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum VerifyResult {

    /** 验签通过。 */
    SUCCESS,

    /** 验签失败（签名错误、过期、重放、未知 keyId 等统一归入）。 */
    INVALID
}
