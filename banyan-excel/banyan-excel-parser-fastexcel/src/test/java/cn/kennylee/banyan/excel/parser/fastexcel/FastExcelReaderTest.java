package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.kennylee.banyan.excel.parser.ParseResult;
import cn.kennylee.banyan.excel.parser.SheetRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 实现模块读回测试：真实 fastexcel 读取 xlsx、表头别名、sheet 选择与表头行数。
 *
 * @author kennylee
 * @since 0.1.0
 */
class FastExcelReaderTest {

    private final List<File> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanup() {
        tempFiles.forEach(File::delete);
    }

    @Test
    @DisplayName("按批次读取 xlsx 并映射字段")
    void readsXlsxWithBatchSize() {
        // given
        File file = writeOrders(5, "订单");

        // when
        List<OrderRow> rows = new ArrayList<>();
        ParseResult result = FastExcelParsers.xlsx(OrderRow.class)
            .batchSize(2)
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(5, result.totalRows());
        assertEquals(3, result.batchCount());
        assertEquals(5, rows.size());
        assertEquals("ORD-1", rows.get(0).getOrderNo());
        assertEquals(0, new BigDecimal("10.50").compareTo(rows.get(0).getAmount()));
    }

    @Test
    @DisplayName("表头别名把中文表头映射到字段")
    void mapsHeaderAlias() {
        // given
        File file = tempFile(".xlsx");
        StarHeaderRow starRow = new StarHeaderRow();
        starRow.setMobile("13800000001");
        FastExcel.write(file, StarHeaderRow.class).sheet("用户").doWrite(List.of(starRow));

        // when
        List<OrderRow> rows = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .headerAlias("* 手机号", "mobile")
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(1, rows.size());
        assertEquals("13800000001", rows.get(0).getMobile());
    }

    @Test
    @DisplayName("按 sheet 名选择")
    void selectsSheetByName() {
        // given
        File file = writeTwoSheets();

        // when
        List<OrderRow> rows = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .sheet(SheetRef.ofName("用户"))
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(1, rows.size());
        assertEquals("U-1", rows.get(0).getOrderNo());
    }

    @Test
    @DisplayName("按 sheet 索引选择")
    void selectsSheetByIndex() {
        // given
        File file = writeTwoSheets();

        // when
        List<OrderRow> rows = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .sheet(SheetRef.ofIndex(1))
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(1, rows.size());
        assertEquals("U-1", rows.get(0).getOrderNo());
    }

    @Test
    @DisplayName("CSV 读取与 xlsx 同入口")
    void readsCsv() throws Exception {
        // given
        File file = tempFile(".csv");
        java.nio.file.Files.writeString(file.toPath(),
            "订单号,手机号,金额\nORD-1,13800000001,10.50\n", java.nio.charset.StandardCharsets.UTF_8);

        // when
        List<OrderRow> rows = new ArrayList<>();
        FastExcelParsers.csv(OrderRow.class)
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(1, rows.size());
        assertEquals("ORD-1", rows.get(0).getOrderNo());
    }

    @Test
    @DisplayName("ignoreEmptyRow 控制空行是否进入批次")
    void controlsEmptyRows() throws Exception {
        // given
        File file = tempFile(".xlsx");
        FastExcel.write(file, WriteOrderRow.class).sheet("订单")
            .doWrite(List.of(orderRow("ORD-1"), new WriteOrderRow(), orderRow("ORD-2")));

        // when
        List<OrderRow> withoutEmpty = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .ignoreEmptyRow(true)
            .read(file, batch -> withoutEmpty.addAll(batch.rows()));
        List<OrderRow> withEmpty = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .ignoreEmptyRow(false)
            .read(file, batch -> withEmpty.addAll(batch.rows()));

        // then
        assertEquals(2, withoutEmpty.size());
        assertEquals(3, withEmpty.size());
    }

    @Test
    @DisplayName("readAll(File) 走文件入口并返回全部行")
    void readAllFromFile() {
        // given
        File file = writeOrders(3, "订单");

        // when
        List<OrderRow> rows = FastExcelParsers.xlsx(OrderRow.class).readAll(file);

        // then
        assertEquals(3, rows.size());
        assertEquals("ORD-1", rows.get(0).getOrderNo());
    }

    @Test
    @DisplayName("两行表头按配置跳过")
    void supportsTwoHeadRows() {
        // given
        File file = tempFile(".xlsx");
        List<List<String>> head = List.of(
            List.of("说明", "订单号"),
            List.of("说明", "手机号"),
            List.of("说明", "金额"));
        List<List<String>> data = List.of(List.of("ORD-9", "13800000009", "9.90"));
        FastExcel.write(file).head(head).sheet("订单").doWrite(data);

        // when
        List<OrderRow> rows = new ArrayList<>();
        FastExcelParsers.xlsx(OrderRow.class)
            .headRowNumber(2)
            .read(file, batch -> rows.addAll(batch.rows()));

        // then
        assertEquals(1, rows.size());
        assertEquals("ORD-9", rows.get(0).getOrderNo());
    }

    private static WriteOrderRow orderRow(String orderNo) {
        WriteOrderRow row = new WriteOrderRow();
        row.setOrderNo(orderNo);
        row.setMobile("13800000001");
        row.setAmount(new BigDecimal("1.00"));
        return row;
    }

    private File writeOrders(int count, String sheetName) {
        List<WriteOrderRow> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            WriteOrderRow row = new WriteOrderRow();
            row.setOrderNo("ORD-" + i);
            row.setMobile("1380000000" + i);
            row.setAmount(new BigDecimal("10.50"));
            rows.add(row);
        }
        File file = tempFile(".xlsx");
        FastExcel.write(file, WriteOrderRow.class).sheet(sheetName).doWrite(rows);
        return file;
    }

    private File writeTwoSheets() {
        File file = tempFile(".xlsx");
        List<WriteOrderRow> orders = new ArrayList<>();
        WriteOrderRow order = new WriteOrderRow();
        order.setOrderNo("O-1");
        order.setMobile("13800000001");
        order.setAmount(new BigDecimal("1.00"));
        orders.add(order);

        List<WriteOrderRow> users = new ArrayList<>();
        WriteOrderRow user = new WriteOrderRow();
        user.setOrderNo("U-1");
        user.setMobile("13800000002");
        user.setAmount(new BigDecimal("2.00"));
        users.add(user);

        try (ExcelWriter writer = FastExcel.write(file, WriteOrderRow.class).build()) {
            writer.write(orders, FastExcel.writerSheet(0, "订单").build());
            writer.write(users, FastExcel.writerSheet(1, "用户").build());
        }
        return file;
    }

    private File tempFile(String suffix) {
        try {
            File file = File.createTempFile("parser-fastexcel-", suffix);
            tempFiles.add(file);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("创建临时文件失败", e);
        }
    }

    /**
     * 读取目标行。
     */
    public static class OrderRow {

        @ExcelProperty("订单号")
        private String orderNo;

        @ExcelProperty("手机号")
        private String mobile;

        @ExcelProperty("金额")
        private BigDecimal amount;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * 写出用行。
     */
    public static class WriteOrderRow {

        @ExcelProperty("订单号")
        private String orderNo;

        @ExcelProperty("手机号")
        private String mobile;

        @ExcelProperty("金额")
        private BigDecimal amount;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * 带星号表头的写出用行。
     */
    public static class StarHeaderRow {

        @ExcelProperty("* 手机号")
        private String mobile;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }
}
