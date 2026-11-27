# banyan-excel-parser-fastexcel

基于 FastExcel 的解析实现：把 xlsx / CSV 输入源流式解码成目标类型的行，按批次推给公共层。

## 一、模块能力

- **xlsx 流式读取**：按 `batchSize` 攒批后推给公共层，解析阶段单线程，不驻留全量数据。
- **表头映射**：优先使用 `headerAlias` 的表头文案 → 字段名；其次匹配字段上的 `@ExcelProperty` 文案或 `@ExcelProperty(index)`；最后按字段名匹配。
- **sheet 选择**：支持按名称或索引读取；表头行数可配置。
- **CSV 读取**：与 xlsx 同一入口，显式指定 `ParseFormat.CSV`，按 UTF-8 解码。
- **类型转换**：支持常见基本类型、包装类型、`BigDecimal`、`LocalDate`、`LocalDateTime` 与枚举；日期接受 ISO、`yyyy/M/d`、`yyyyMMdd`，日期时间接受 ISO、`yyyy-MM-dd HH:mm:ss`、`yyyy/M/d HH:mm:ss`，布尔接受 `true`/`false`、`1`/`0`、`是`/`否`；其它类型视为不支持。
- **compile 直传 FastExcel**：引入实现模块即可用，不需要再单独声明 FastExcel 或 POI。

## 二、引入

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-excel-parser-fastexcel</artifactId>
</dependency>
```

版本由项目 BOM 管理；引入后 FastExcel 及其传递依赖（如 POI）会进入使用方依赖链。

## 三、快速开始

```java
ParseResult result = FastExcelParsers.xlsx(OrderRow.class)
    .batchSize(500)
    .read(inputStream, batch -> saveBatch(batch.rows()));
```

CSV：

```java
FastExcelParsers.csv(OrderRow.class)
    .read(file, batch -> handle(batch.rows()));
```

表头别名：

```java
FastExcelParsers.xlsx(OrderRow.class)
    .headerAlias("* 用户手机号", "mobile")
    .read(file, batch -> handle(batch.rows()));
```

## 四、表头与 sheet

- `headRowNumber` 默认 1；多行表头时用最后一行的文案建立列映射。
- `sheet(SheetRef.ofName("订单"))` 与 `sheet(SheetRef.ofIndex(1))` 二选一，后设覆盖先设。
- 表头列没有任何字段匹配时，该列被忽略；字段没有匹配列时保持默认值。

## 五、CSV

CSV 与 xlsx 走同一读取入口，公共层的能力（批次、保序、并发、`maxRows`、别名）都适用；CSV 没有 sheet 语义，`sheet` 配置对 CSV 无影响。编码固定 UTF-8。

## 六、依赖边界

- 只有本模块 compile 依赖 `cn.idev.excel:fastexcel`；公共模块 `banyan-excel-parser` 的主依赖链看不到它。
- FastExcel 的传递依赖（POI 等）随本模块进入使用方，由使用方的 BOM 与 Maven 调解。
- 本模块不是 starter，没有自动装配与配置项。

## 七、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| FastExcel 读取入口 | [`src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/`](src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/) | `FastExcelRowReader`、`FastExcelParsers` |
| 表头与字段映射 | [`src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/`](src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/) | `RowMapper` |
| 类型转换 | [`src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/`](src/main/java/cn/kennylee/banyan/excel/parser/fastexcel/) | `ValueConverters` |

公共契约与参数说明见 `banyan-excel-parser` 模块的 README。
