package cn.kennylee.banyan.excel.exporter;

import java.io.File;
import java.time.Duration;

/**
 * 一次同步导出的结果。
 *
 * @param file         生成的临时文件；空数据且关闭产出时为 {@code null}
 * @param totalRecords 总记录数
 * @param spentTime    导出耗时
 * @param truncated    是否因显式设置的最大记录数触顶而截断
 * @author kennylee
 * @since 0.1.0
 */
public record ExcelExportResult(File file, int totalRecords, Duration spentTime, boolean truncated) {

    /**
     * 删除生成的临时文件。
     *
     * @return 文件存在且删除成功时返回 {@code true}
     */
    public boolean deleteFile() {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }
}
