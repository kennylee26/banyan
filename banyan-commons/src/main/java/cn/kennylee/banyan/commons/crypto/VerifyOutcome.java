package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.util.Objects;

/**
 * 验签详细结果：成功时原因为空，失败时原因必填。
 *
 * <p>供接入层写具体日志与联调使用；对外响应仍应使用
 * {@link ReplayGuard#verify(SignRequest, java.util.Map)} 的
 * {@link VerifyResult}，默认不暴露原因。</p>
 *
 * @param result 验签结果
 * @param reason 失败原因；成功时为 null
 * @author kennylee
 * @since 0.1.0
 */
public record VerifyOutcome(VerifyResult result, FailureReason reason) {

    /**
     * 校验结果与原因的组合是否自洽。
     *
     * @throws IllegalArgumentException 成功却带原因，或失败却没有原因
     */
    public VerifyOutcome {
        Objects.requireNonNull(result, "result");
        if ((result == VerifyResult.SUCCESS) != (reason == null)) {
            throw new IllegalArgumentException(
                    Messages.get(ErrorCode.SIGN_REQUEST_INVALID, "reason"));
        }
    }

    /**
     * 构造成功结果。
     *
     * @return 成功且无原因的验签结果
     */
    public static VerifyOutcome success() {
        return new VerifyOutcome(VerifyResult.SUCCESS, null);
    }

    /**
     * 构造失败结果。
     *
     * @param reason 失败原因
     * @return 失败且带原因的验签结果
     */
    public static VerifyOutcome failure(FailureReason reason) {
        return new VerifyOutcome(VerifyResult.INVALID, Objects.requireNonNull(reason, "reason"));
    }

    /**
     * 是否验签通过。
     *
     * @return 通过返回 true
     */
    public boolean isSuccess() {
        return result == VerifyResult.SUCCESS;
    }
}
