package cn.kennylee.banyan.data.mysql.das;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批量更新列数上限配置解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class MaxUpdateColumnsTest {

    /** 未配置时默认 6。 */
    @Test
    void defaultsToSix() {
        // when
        assertThat(BaseDas.resolveMaxUpdateColumns(null)).isEqualTo(6);
        assertThat(BaseDas.resolveMaxUpdateColumns("")).isEqualTo(6);
        assertThat(BaseDas.resolveMaxUpdateColumns("  ")).isEqualTo(6);
    }

    /** 合法值（1-32）原样生效。 */
    @Test
    void acceptsValidValue() {
        // when
        assertThat(BaseDas.resolveMaxUpdateColumns("1")).isEqualTo(1);
        assertThat(BaseDas.resolveMaxUpdateColumns("12")).isEqualTo(12);
        assertThat(BaseDas.resolveMaxUpdateColumns("32")).isEqualTo(32);
    }

    /** -1、0、非数字与超过硬上限的 33 都回退 6 并输出 WARN。 */
    @Test
    void fallsBackOnIllegalValue(CapturedOutput output) {
        // when
        assertThat(BaseDas.resolveMaxUpdateColumns("-1")).isEqualTo(6);
        assertThat(BaseDas.resolveMaxUpdateColumns("0")).isEqualTo(6);
        assertThat(BaseDas.resolveMaxUpdateColumns("abc")).isEqualTo(6);
        assertThat(BaseDas.resolveMaxUpdateColumns("33")).isEqualTo(6);

        // then
        assertThat(output).contains("非法").contains("WARN");
    }
}
