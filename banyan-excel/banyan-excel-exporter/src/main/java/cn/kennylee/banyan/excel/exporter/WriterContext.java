package cn.kennylee.banyan.excel.exporter;

import java.io.File;
import java.io.InputStream;

/**
 * 行写入器的打开上下文。
 *
 * @param target   写出目标临时文件
 * @param rowType  表头类，用于列定义
 * @param template 模板输入流，可为 {@code null}；由导出流程负责关闭
 * @param <T>      行数据类型
 * @author kennylee
 * @since 0.1.0
 */
public record WriterContext<T>(File target, Class<T> rowType, InputStream template) {
}
