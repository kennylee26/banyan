package cn.kennylee.banyan.excel.exporter;

import java.io.InputStream;

/**
 * 导出流程的不可变配置。
 *
 * @param format           导出格式
 * @param rowType          表头类
 * @param template         模板输入流，可为 {@code null}
 * @param batchSize        每批行数
 * @param maxExportRecords 最大导出条数，{@link ExcelExporter#UNLIMITED} 表示不限
 * @param sheetCapacity    单 sheet 数据行容量
 * @param writeEmptyData   空数据是否仍产出文件
 * @param <T>              行数据类型
 * @author kennylee
 * @since 0.1.0
 */
record ExportOptions<T>(ExportFormat format, Class<T> rowType, InputStream template,
                        int batchSize, int maxExportRecords, int sheetCapacity, boolean writeEmptyData) {
}
