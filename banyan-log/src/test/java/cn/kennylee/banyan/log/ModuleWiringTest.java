package cn.kennylee.banyan.log;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块接线测试：新增模块必须同时出现在根 pom、BOM 与两份 README 中。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ModuleWiringTest {

    /** 仓库根目录（测试运行时的工作目录是模块目录）。 */
    private static final Path ROOT = Paths.get("..").toAbsolutePath().normalize();

    /** 根 pom 的 modules 列表。 */
    @Test
    void rootPomListsModule() throws IOException {
        // when
        assertThat(readRoot("pom.xml")).contains("<module>banyan-log</module>");
    }

    /** BOM 的 dependencyManagement。 */
    @Test
    void bomManagesModule() throws IOException {
        // when
        assertThat(readRoot("banyan-bom/pom.xml")).contains("<artifactId>banyan-log</artifactId>");
    }

    /** 两份 README 的模块表。 */
    @Test
    void readmesListModule() throws IOException {
        // when
        assertThat(readRoot("README.md")).contains("`banyan-log`");
        assertThat(readRoot("README.zh-CN.md")).contains("`banyan-log`");
    }

    /** 读取仓库根目录下的文件。 */
    private String readRoot(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
