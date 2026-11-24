package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 列数上限配置非法时的告警测试：方言不支持也要读到配置并告警。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = TestApp.class,
        properties = "banyan.data-mysql.max-update-columns=abc")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class BatchUpdateInvalidConfigTest {

    @BeforeEach
    void fixLocale() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DasQueryStrategyRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    /** 方言被判为不支持时，非法配置仍要回退 6 并输出 WARN。 */
    @Test
    void warnsInvalidConfigEvenWhenDialectUnsupported(CapturedOutput output) {
        // given
        DialectControlledUserDas das = new DialectControlledUserDas(userMapper, registry, false);
        das.setApplicationContext(applicationContext);
        UserEo user = new UserEo();
        user.setUserName("abel");
        user.setAge(70);
        das.save(user);
        user.setAge(71);

        // when
        assertThat(das.updateBatchById(List.of(user))).isEqualTo(-1);

        // then
        assertThat(output).contains("非法");
    }
}
