# banyan-excel

表格读写模块族：零依赖公共契约 + 可替换的真实实现，按批次流式导出与解析 xlsx / CSV。

## 一、模块能力

- **分页批量导出**：使用方只实现「按游标取一批数据」，库负责逐批写出、计数、上限与清理，内存占用与总行数解耦。
- **流式分批解析**：使用方只实现批次处理器，库按批次大小读取并回调；解析阶段单线程流式，处理阶段可配并发度与在途批次上限，内存占用与总行数解耦。
- **保序可选**：解析默认按文件行序串行回调；关掉保序可并行处理，批次自带起始行号供使用方自行排序。
- **公共契约零依赖**：契约模块主依赖链没有 Spring、POI 或任何 Excel 第三方库，可脱离具体实现单独编译与测试。
- **默认不限条数且可设防**：导出不设上限时写出全部数据；需要时按条数截断并在结果中标记。解析的 `maxRows` 触顶同样停止并标记 `truncated`。
- **xlsx 与 CSV 双格式**：导出侧 xlsx 支持模板与按容量自动分 sheet、CSV 为纯文本旁路；解析侧支持 xlsx 流式读取，CSV 视实现模块能力提供。
- **纯调用式工具库**：不注册任何 Spring bean、没有自动装配与配置开关，读写由同步调用触发。

适合需要把大量表格数据导出或导入、又不想把业务系统里的异步 / 上传 / 记录 / 存库逻辑搬进库的项目；若只需要一次性读写少量数据、或已经有自己的表格框架，引入前先确认这套批次契约是否划算。

## 二、模块

| 模块 | 角色 | 依赖 |
|---|---|---|
| `banyan-excel-exporter` | 零依赖公共契约：批次数据源、行写入器 SPI、导出流程与结果 | `banyan-commons` |
| `banyan-excel-exporter-fastexcel` | 基于 FastExcel 的真实写出：xlsx 模板、自动分 sheet、CSV | `excel-exporter` + `fastexcel` |
| `banyan-excel-parser` | 零依赖解析契约：推送式分批读取、批次处理器、行读取器 SPI 与结果 | `banyan-commons` |
| `banyan-excel-parser-fastexcel` | 基于 FastExcel 的真实读取：xlsx 流式读取、表头别名、sheet 选择与 CSV | `excel-parser` + `fastexcel` |

依赖方向单向：实现模块依赖契约模块，契约模块不感知任何实现。引入实现模块即可使用全部能力。

## 三、引入

导出：

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-exporter-fastexcel</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

解析：

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-parser-fastexcel</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

只引契约模块（`excel-exporter` / `excel-parser`）时不含任何 Excel 库，需自行提供行写入器 / 行读取器实现。

## 四、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 公共导出契约与流程 | [`banyan-excel-exporter/`](banyan-excel-exporter/) | `BatchSource`、`ExcelRowWriter`、`ExcelExporter`、`ExcelExporterBuilder`、`ExcelExportResult` |
| FastExcel 写出实现 | [`banyan-excel-exporter-fastexcel/`](banyan-excel-exporter-fastexcel/) | `FastExcelExporters`、`FastExcelXlsxRowWriter`、`FastExcelCsvRowWriter` |
| 公共解析契约与流程 | [`banyan-excel-parser/`](banyan-excel-parser/) | `ExcelParser`、`ExcelParserBuilder`、`BatchHandler`、`ReadBatch`、`ParseResult`、`ExcelRowReader` |
| FastExcel 读取实现 | [`banyan-excel-parser-fastexcel/`](banyan-excel-parser-fastexcel/) | `FastExcelParsers`、`FastExcelRowReader` |

本模块是聚合 POM，自身不含 `src`；实现分散在四个子模块中。本模块族没有 `banyan.*` 配置项，也不出现在 `CONFIGURATION.md` 总表中。
