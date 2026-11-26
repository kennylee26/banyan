# banyan-excel-exporter

导出公共契约：批次数据源、行写入器 SPI、导出游标、流程模板与结果，主依赖零 Spring、零第三方。

## 一、模块能力

- **批次数据源契约**：使用方实现 `BatchSource` 按游标返回一批数据，库不关心数据来自数据库还是远程接口。
- **逐批流式写出**：流程模板按批次取数、写出一批后释放引用，不把全部行累积在内存，大数据量不会因驻留而溢出。
- **默认不限条数与显式设防**：不设上限时导出完整数据；设置最大条数后触顶静默停止并在结果中标记截断。
- **自动分 sheet**：xlsx 按 `sheetCapacity`（默认 1,048,575 数据行，为表头留出 1 行）累计触顶后切换 sheet，使用方无感；带模板且模板表头不止一行时，请按模板行数下调容量。
- **可预测的清理**：失败时删除临时文件并抛本库异常；空数据默认仍产出文件且可关闭；写出器与模板流一定被关闭。

适合在已有导出框架之外想要一套稳定批次契约的项目，或需要对导出过程做单元测试（可用纯 JDK 替身写出器）的场景；它本身不提供任何真实 Excel 写出，需要搭配实现模块。

## 二、引入

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-exporter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

本模块主依赖只有 `banyan-commons`（纯 JDK），不含 Spring、POI 或任何 Excel 库。

## 三、配置

本模块是纯调用式工具库，没有自动装配，也没有 `banyan.*` 配置项，不进入 `CONFIGURATION.md` 总表。批次大小、上限、sheet 容量、是否产出空文件都在构建时显式设置。

## 四、用法

实现数据源，交给导出流程：

```java
public class OrderBatchSource implements BatchSource<OrderRow> {
    @Override
    public List<OrderRow> loadNextBatch(ExportCursor cursor, int batchSize) {
        // 按 cursor.exportedCount() 或 cursor.prevPageLastId() 查询下一批
        return orderQuery.findPage(cursor, batchSize);
    }
}
```

搭配实现模块完成真实导出：

```java
ExcelExportResult result = FastExcelExporters.xlsx(new OrderBatchSource(), OrderRow.class)
    .batchSize(500)
    .export();
try {
    upload(result.file());
} finally {
    result.deleteFile();
}
```

只引本模块时，用自定义 `ExcelRowWriter` 构造：

```java
ExcelExportResult result = ExcelExporterBuilder
    .of(batchSource, myRowWriter, OrderRow.class)
    .format(ExportFormat.CSV)
    .export();
```

## 五、边界与设计取舍

- **同步导出**：`export()` 是同步调用，异步提交、MDC 透传、导出记录、上传、下载地址都留在使用方，不进入本库。
- **结果只有文件**：返回临时 `File` 而非 `OutputStream`，因为典型用途是「生成 → 上传 → 删除」，文件能同时提供大小与清理能力。
- **不猜泛型**：表头 `Class<T>` 由调用方显式传入，不靠反射解析多级泛型。
- **游标分页可选**：默认按「已导出行数」推进；需要 keyset 分页时，实现 `BatchSource.lastIdOf` 让库把上一批末条 ID 带进下一批。
- **截断语义**：只有显式设置了 `maxExportRecords` 且达到该值才标记 `truncated=true`，默认不限。

## 六、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 批次数据源契约与游标 | [`exporter/`](src/main/java/cn/kennylee/banyan/excel/exporter/) | `BatchSource`、`ExportCursor` |
| 行写入器 SPI 与打开上下文 | [`exporter/`](src/main/java/cn/kennylee/banyan/excel/exporter/) | `ExcelRowWriter`、`WriterContext` |
| 导出流程、构建器与结果 | [`exporter/`](src/main/java/cn/kennylee/banyan/excel/exporter/) | `ExcelExporter`、`ExcelExporterBuilder`、`ExcelExportResult` |
| 输出格式 | [`exporter/`](src/main/java/cn/kennylee/banyan/excel/exporter/) | `ExportFormat` |
