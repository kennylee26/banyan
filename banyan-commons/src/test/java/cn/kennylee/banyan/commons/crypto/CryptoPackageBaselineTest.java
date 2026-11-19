package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 零破坏基线：现有包与类不动、commons 保持零第三方、新增能力只做加法。
 *
 * @author kennylee
 * @since 0.1.0
 */
class CryptoPackageBaselineTest {

    /** 现有 13 个公开类型。 */
    private static final String[] EXISTING_TYPES = {
        "cn.kennylee.banyan.commons.error.BanyanException",
        "cn.kennylee.banyan.commons.error.ErrorCode",
        "cn.kennylee.banyan.commons.error.MessageResolver",
        "cn.kennylee.banyan.commons.error.Messages",
        "cn.kennylee.banyan.commons.error.ResourceBundleMessageResolver",
        "cn.kennylee.banyan.commons.id.IdProvider",
        "cn.kennylee.banyan.commons.id.UuidIdProvider",
        "cn.kennylee.banyan.commons.util.Base64s",
        "cn.kennylee.banyan.commons.util.Beans",
        "cn.kennylee.banyan.commons.util.CollUtils",
        "cn.kennylee.banyan.commons.util.Hexs",
        "cn.kennylee.banyan.commons.util.Masks",
        "cn.kennylee.banyan.commons.util.Strings"
    };

    /** 现有类型仍在原包，不因新增 crypto 包而移动。 */
    @Test
    void existingTypesStayInPlace() {
        for (String type : EXISTING_TYPES) {
            assertNotNull(load(type), type);
        }
    }

    /** commons 的 pom 只允许 test 依赖。 */
    @Test
    void pomDeclaresOnlyTestDependencies() throws IOException {
        String pom = Files.readString(Paths.get("pom.xml"));

        assertFalse(pom.contains("<scope>compile</scope>"));
        assertFalse(pom.contains("<artifactId>hutool"));
        assertFalse(pom.contains("<artifactId>bouncycastle"));
        assertFalse(pom.contains("<artifactId>jackson"));
        assertFalse(pom.contains("<artifactId>spring-boot"));
    }

    /** commons 不提供自动装配。 */
    @Test
    void hasNoAutoConfigurationImports() {
        assertNull(getClass().getClassLoader().getResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
    }

    /** crypto 包已创建（本任务先红，1.2 创建后转绿）。 */
    @Test
    void cryptoPackageExists() {
        assertNotNull(getClass().getClassLoader().getResource(
                "cn/kennylee/banyan/commons/crypto/package-info.class"));
    }

    /** 按类名加载类型。 */
    private Class<?> load(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
