# banyan-excel-exporter-fastexcel

基于 FastExcel 的导出实现：xlsx 模板填充、按容量自动分 sheet 与 CSV 旁路。

## 一、模块能力

- **引入即可导出**：依赖本模块即同时获得公共契约与 FastExcel 写出能力，不需要再声明 fastexcel / POI 坐标。
- **xlsx 模板填充**：传入模板输入流时按模板样式填充数据、不重复写出表头；不传模板时按表头类写出表头。
- **自动分 sheet**：超过 sheet 容量时由公共流程驱动切换，本模块只负责把下一批写到新的 sheet。
- **CSV 旁路**：同一坐标提供 CSV 写出，适合数据量大到不想受 xlsx 单 sheet 上限约束的场景，无模板、无样式、无 sheet。
- **真实写出可读回**：集成测试用最小模板导出并读回，核对表头与数据行。

适合想直接拿到可用导出能力、不想自己适配 Excel 库的项目；如果只需要契约或想换用别的写出库，只引 `banyan-excel-exporter` 并自行实现 `ExcelRowWriter`。

## 二、引入

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-exporter-fastexcel</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

本模块以 **compile** 方式依赖 `cn.idev.excel:fastexcel`（版本由本库 `pom.xml` 统一管理），并传递引入其依赖（POI、poi-ooxml、commons-csv、ehcache、commons-io、slf4j-api、fastexcel-support）。使用方若已锁定其它 POI 版本，需自行确认兼容；具体传递版本以 `mvn dependency:tree` 为准。

## 三、配置

本模块没有 java config、没有自动装配，也没有 `banyan.*` 配置项。格式、批次大小、上限、sheet 容量都通过构建器设置。

## 四、用法

```java
// xlsx，带模板
ExcelExportResult xlsx = FastExcelExporters.xlsx(new OrderBatchSource(), OrderRow.class)
    .template(templateInputStream)
    .batchSize(500)
    .sheetCapacity(1_048_575)
    .export();

// CSV，无模板、无 sheet 上限
ExcelExportResult csv = FastExcelExporters.csv(new OrderBatchSource(), OrderRow.class)
    .batchSize(1000)
    .export();
```

按格式选择：

```java
ExcelExporterBuilder<OrderRow> builder = FastExcelExporters.of(ExportFormat.CSV, source, OrderRow.class);
```

## 五、边界与已知行为

- **CSV 不等于 xlsx**：CSV 无模板、无单元格样式、无 sheet 概念；表头仍由表头类的列定义决定。
- **模板与分 sheet**：带模板时，首个 sheet 保留模板样式；切换到新 sheet 后既无模板样式也不额外写表头，跨 sheet 的数据完整性由公共流程保证。若需要每个 sheet 都有表头，请改用无模板模式（表头由表头类生成）或自行控制 `sheetCapacity` 只用一个 sheet。
- **模板流由调用方或流程关闭**：通过 Builder 传入的模板流由导出流程在结束时关闭；调用方自行打开的资源仍由调用方负责。
- **传递依赖**：fastexcel 会带入 POI 等库，这是本模块「引入即可用」的代价；只想要契约的请改用 `banyan-excel-exporter`。
- **CSV 入口**：通过 `ExcelWriterBuilder.excelType(ExcelTypeEnum.CSV)` 落地，与 xlsx 共用同一个 `ExcelWriter` 生命周期。

## 六、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 导出入口 | [`fastexcel/`](src/main/java/cn/kennylee/banyan/excel/exporter/fastexcel/) | `FastExcelExporters` |
| xlsx 模板与自动分 sheet | [`fastexcel/`](src/main/java/cn/kennylee/banyan/excel/exporter/fastexcel/) | `FastExcelXlsxRowWriter` |
| CSV 旁路 | [`fastexcel/`](src/main/java/cn/kennylee/banyan/excel/exporter/fastexcel/) | `FastExcelCsvRowWriter` |
