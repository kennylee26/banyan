package cn.kennylee.banyan.excel.exporter;

/**
 * 导出文件格式。
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum ExportFormat {

    /** 带样式的 Excel 工作簿，支持模板与按容量自动分 sheet。 */
    XLSX(".xlsx"),

    /** 纯文本逗号分隔文件，无模板、无样式、无 sheet 行数上限。 */
    CSV(".csv");

    /** 文件后缀，含点号。 */
    private final String suffix;

    ExportFormat(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 获取文件后缀。
     *
     * @return 文件后缀，含点号
     */
    public String suffix() {
        return suffix;
    }

    /**
     * 该格式是否按 sheet 容量拆分。
     *
     * @return xlsx 返回 {@code true}，CSV 返回 {@code false}
     */
    public boolean supportsMultipleSheets() {
        return this == XLSX;
    }
}
