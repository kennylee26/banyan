package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.kennylee.banyan.excel.parser.ExcelParserBuilder;
import cn.kennylee.banyan.excel.parser.ParseFormat;

/**
 * FastExcel 解析入口的静态工厂。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class FastExcelParsers {

    private FastExcelParsers() {
    }

    /**
     * 创建 xlsx 解析配置。
     *
     * @param rowType 目标行类型
     * @param <T>     行类型
     * @return 解析配置 Builder
     */
    public static <T> ExcelParserBuilder<T> xlsx(Class<T> rowType) {
        return ExcelParserBuilder.of(new FastExcelRowReader<>(), rowType).format(ParseFormat.XLSX);
    }

    /**
     * 创建 CSV 解析配置；是否可用取决于实现模块对 CSV 的支持。
     *
     * @param rowType 目标行类型
     * @param <T>     行类型
     * @return 解析配置 Builder
     */
    public static <T> ExcelParserBuilder<T> csv(Class<T> rowType) {
        return ExcelParserBuilder.of(new FastExcelRowReader<>(), rowType).format(ParseFormat.CSV);
    }
}
