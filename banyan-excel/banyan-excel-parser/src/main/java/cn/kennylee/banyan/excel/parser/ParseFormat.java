package cn.kennylee.banyan.excel.parser;

/**
 * 解析的输入格式。
 *
 * <p>默认按 xlsx 读取；CSV 需显式指定，且取决于实现模块是否提供该能力。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum ParseFormat {

    /** Office Open XML 工作簿。 */
    XLSX,

    /** 逗号分隔文本。 */
    CSV
}
