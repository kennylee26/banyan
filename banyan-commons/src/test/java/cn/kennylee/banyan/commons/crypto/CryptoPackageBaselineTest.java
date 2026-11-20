package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        // when
        for (String type : EXISTING_TYPES) {
            assertNotNull(load(type), type);
        }
    }

    /** commons 的 pom 只允许 test 依赖，主依赖链仍为零第三方。 */
    @Test
    void pomDeclaresOnlyTestDependencies() throws IOException {
        // given
        String pom = Files.readString(Paths.get("pom.xml"));

        // when
        assertFalse(pom.contains("<scope>compile</scope>"));
        assertFalse(pom.contains("<artifactId>hutool"));
        assertFalse(pom.contains("<artifactId>bouncycastle"));
        assertFalse(pom.contains("<artifactId>jackson"));

        Matcher dependencies = Pattern
                .compile("<dependency>(.*?)</dependency>", Pattern.DOTALL)
                .matcher(pom);
        int scoped = 0;
        while (dependencies.find()) {
            String dependency = dependencies.group(1);
            assertTrue(dependency.contains("<scope>test</scope>"), dependency.trim());
            scoped++;
        }
        assertTrue(scoped >= 2);
    }

    /** 本模块源码树下不产出自动装配资源（test 依赖自带来的不算）。 */
    @Test
    void hasNoAutoConfigurationImports() throws IOException {
        // when
        assertFalse(Files.exists(Paths.get("src/main/resources")
                .resolve("META-INF/spring")
                .resolve("org.springframework.boot.autoconfigure.AutoConfiguration.imports")));
    }

    /** crypto 包已创建（本任务先红，1.2 创建后转绿）。 */
    @Test
    void cryptoPackageExists() {
        // when
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
