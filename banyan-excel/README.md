# banyan-excel

表格导出模块族：零依赖公共契约 + 可替换的真实写出实现，按批次流式导出 xlsx / CSV。

## 一、模块能力

- **分页批量导出**：使用方只实现「按游标取一批数据」，库负责逐批写出、计数、上限与清理，内存占用与总行数解耦。
- **公共契约零依赖**：契约模块主依赖链没有 Spring、POI 或任何 Excel 第三方库，可脱离具体实现单独编译与测试。
- **默认不限条数且可设防**：不设上限时导出全部数据；需要时按条数截断并在结果中标记，不悄悄丢数据。
- **xlsx 与 CSV 双格式**：xlsx 支持模板与按容量自动分 sheet；CSV 作为无行数上限的纯文本旁路。
- **纯调用式工具库**：不注册任何 Spring bean、没有自动装配与配置开关，导出由同步调用触发。

适合需要把大量数据导出成表格、又不想把业务系统里的异步 / 上传 / 记录逻辑搬进库的项目；若只需要一次性导出少量数据、或已经有自己的导出框架，引入前先确认这套批次契约是否划算。

## 二、模块

| 模块 | 角色 | 依赖 |
|---|---|---|
| `banyan-excel-exporter` | 零依赖公共契约：批次数据源、行写入器 SPI、导出流程与结果 | `banyan-commons` |
| `banyan-excel-exporter-fastexcel` | 基于 FastExcel 的真实写出：xlsx 模板、自动分 sheet、CSV | `excel-exporter` + `fastexcel` |

依赖方向单向：实现模块依赖契约模块，契约模块不感知任何实现。引入实现模块即可使用全部能力。

## 三、引入

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-exporter-fastexcel</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

只引 `banyan-excel-exporter` 时不含任何 Excel 库，需自行提供 `ExcelRowWriter` 实现。

## 四、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 公共导出契约与流程 | [`banyan-excel-exporter/`](banyan-excel-exporter/) | `BatchSource`、`ExcelRowWriter`、`ExcelExporter`、`ExcelExporterBuilder`、`ExcelExportResult` |
| FastExcel 写出实现 | [`banyan-excel-exporter-fastexcel/`](banyan-excel-exporter-fastexcel/) | `FastExcelExporters`、`FastExcelXlsxRowWriter`、`FastExcelCsvRowWriter` |

本模块是聚合 POM，自身不含 `src`；实现分散在两个子模块中。本模块族没有 `banyan.*` 配置项，也不出现在 `CONFIGURATION.md` 总表中。
