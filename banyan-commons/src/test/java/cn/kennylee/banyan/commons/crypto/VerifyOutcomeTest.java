package cn.kennylee.banyan.commons.crypto;

import cn.kennylee.banyan.commons.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验签详细结果模型测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class VerifyOutcomeTest {

    /** 成功结果不携带原因。 */
    @Test
    void successHasNoReason() {
        // when
        VerifyOutcome outcome = VerifyOutcome.success();

        // then
        assertEquals(VerifyResult.SUCCESS, outcome.result());
        assertNull(outcome.reason());
        assertTrue(outcome.isSuccess());
    }

    /** 失败结果必须携带原因。 */
    @Test
    void failureKeepsReason() {
        // when
        VerifyOutcome outcome = VerifyOutcome.failure(FailureReason.EXPIRED);

        // then
        assertEquals(VerifyResult.INVALID, outcome.result());
        assertEquals(FailureReason.EXPIRED, outcome.reason());
        assertFalse(outcome.isSuccess());
    }

    /** 失败原因自带对应的错误码。 */
    @Test
    void failureReasonCarriesErrorCode() {
        // when
        ErrorCode errorCode = FailureReason.EXPIRED.errorCode();

        // then
        assertEquals(ErrorCode.OPENAPI_SIGN_EXPIRED, errorCode);
    }

    /** 结果与原因组合不一致时立即失败。 */
    @Test
    void rejectsInconsistentCombination() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> new VerifyOutcome(VerifyResult.SUCCESS, FailureReason.EXPIRED));
        assertThrows(IllegalArgumentException.class,
                () -> new VerifyOutcome(VerifyResult.INVALID, null));
    }
}
