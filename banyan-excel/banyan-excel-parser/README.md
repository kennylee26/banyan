# banyan-excel-parser

零依赖的 Excel 解析公共契约：推送式分批读取、批次处理器、行读取器 SPI、限制项与结果。主依赖链只有 `banyan-commons`，不含 Spring、POI 或任何 Excel 第三方库。

## 一、模块能力

- **流式分批读取**：按 `batchSize` 把行推给批次处理器，库内不驻留全量数据；便捷方法 `readAll` 全量返回，仅适合小文件。
- **并发与保序**：`parallelism` 默认 `1`（不创建额外线程），`maxPendingBatches` 限制在途批次数并形成背压；`ordered` 默认 `true`，批次按文件行序串行回调，关闭后可并发处理并用批次行号自行排序。
- **限制项**：sheet 名或索引、`headRowNumber`、`maxRows`（触顶截断并标记）、`batchSize`、`ignoreEmptyRow`。
- **表头别名**：`headerAlias(表头文案, 字段名)`，不要求 DTO 带注解；公共层不引用 `@ExcelProperty`。
- **错误可控**：批次处理器异常默认终止读取，可配 `onError` 记录后继续；单行转换失败默认快速失败，可配 `onConvertError` 收集并跳过。
- **纯调用式工具库**：不注册 Spring bean、没有自动装配、没有 `banyan.*` 配置项，读写由同步调用触发。

## 二、快速开始

引入实现模块（见 `banyan-excel-parser-fastexcel`）后：

```java
ParseResult result = FastExcelParsers.xlsx(OrderRow.class)
    .batchSize(500)
    .read(inputStream, batch -> saveBatch(batch.rows()));
```

只用公共契约、自行提供 `ExcelRowReader` 实现时：

```java
ParseResult result = ExcelParserBuilder
    .of(myRowReader, OrderRow.class)
    .batchSize(500)
    .read(inputStream, batch -> saveBatch(batch.rows()));
```

全量便捷方法：

```java
List<OrderRow> rows = FastExcelParsers.xlsx(OrderRow.class).readAll(file);
```

`readAll` 会把全部行驻留内存，只适合小文件；大数据量请用批次处理器。

## 三、参数

| 方法 | 默认 | 说明 |
|---|---|---|
| `batchSize(int)` | `500` | 每个批次的期望行数，由实现模块攒批 |
| `maxRows(int)` | `-1`（不限） | 触顶后停止读取，结果 `truncated=true`，不抛异常 |
| `sheet(SheetRef)` | 第一个 sheet | `SheetRef.ofName` / `SheetRef.ofIndex` 二选一，后设覆盖先设 |
| `headRowNumber(int)` | `1` | 表头行数，必须大于等于 1 |
| `ignoreEmptyRow(boolean)` | `true` | 是否忽略空行 |
| `headerAlias(String, String)` | 无 | 表头文案到字段名的映射 |
| `ordered(boolean)` | `true` | 是否按文件行序串行回调 |
| `parallelism(int)` | `1` | 处理阶段并发度；1 表示串行 |
| `maxPendingBatches(int)` | `parallelism × 2` | 在途批次上限，必须大于 0 |
| `onError(Consumer<RowError>)` | 无 | 配置后处理器异常记录并继续 |
| `onConvertError(Consumer<RowError>)` | 无 | 配置后转换失败记录并跳过 |

配置非法（批次大小 ≤ 0、`maxRows` 为 0 或小于 -1、并发度 ≤ 0 等）会在读取前抛 `BanyanException`，错误码 `PARSE_CONFIG_INVALID`。

## 四、结果与失败行

`read` 返回 `ParseResult`：`totalRows`、`batchCount`、`spentTime`、`truncated`、`errors`。`errors` 的元素是 `RowError(rowIndex, message, cause)`，行号是文件物理行号（从 1 开始、计入表头）。

`ReadBatch` 携带 `firstRowIndex`，因此关闭保序后也能按行号还原顺序：

```java
List<ReadBatch<OrderRow>> batches = new CopyOnWriteArrayList<>();
FastExcelParsers.xlsx(OrderRow.class)
    .ordered(false)
    .parallelism(4)
    .batchSize(50)
    .read(file, batches::add);
List<OrderRow> rows = batches.stream()
    .sorted(Comparator.comparingInt(ReadBatch::firstRowIndex))
    .flatMap(batch -> batch.rows().stream())
    .toList();
```

只依赖批次起始行号单调，不假定批内行号连续：空行或转换失败行被跳过时，批内行号会不连续。

库不负责存库、事务、Redis 或业务校验；失败数据直接通过 `ParseResult.errors` 或使用方容器返回。

## 五、输入源与关闭责任

`read(InputStream)` 不关闭调用方传入的流；`read(File)` 由库打开并关闭文件流。`readAll` 遵循同样规则。

## 六、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 推送式读取流程 | [`src/main/java/cn/kennylee/banyan/excel/parser/`](src/main/java/cn/kennylee/banyan/excel/parser/) | `ExcelParser`、`ExcelParserBuilder` |
| 批次与行号 | [`src/main/java/cn/kennylee/banyan/excel/parser/`](src/main/java/cn/kennylee/banyan/excel/parser/) | `BatchHandler`、`ReadBatch`、`RowSink` |
| 限制与结果 | [`src/main/java/cn/kennylee/banyan/excel/parser/`](src/main/java/cn/kennylee/banyan/excel/parser/) | `SheetRef`、`ParseResult`、`RowError`、`ParseFormat` |
| 读取器 SPI | [`src/main/java/cn/kennylee/banyan/excel/parser/`](src/main/java/cn/kennylee/banyan/excel/parser/) | `ExcelRowReader`、`ReaderContext` |

本模块是纯调用式工具库，不是 starter；没有 `banyan.*` 配置项，也不出现在 `CONFIGURATION.md` 总表中。真实读取能力见 `banyan-excel-parser-fastexcel`。
