package cn.kennylee.banyan.data.mysql.mybatis.support;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.util.CollUtils;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import java.util.Collections;
import java.util.List;

/**
 * SQL 文本构造工具。只负责拼 SQL，不执行、不持有连接。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SqlBuilder {

    /** CASE WHEN 批量更新 SQL 的初始缓冲长度。 */
    private static final int UPDATE_SQL_INITIAL_CAPACITY = 64;

    /** 每个 WHEN 分支估算的字符数，用于预留缓冲。 */
    private static final int UPDATE_SQL_PER_BRANCH = 24;

    private SqlBuilder() {
    }

    /**
     * 构造多值 INSERT：{@code INSERT INTO t (c1,c2) VALUES (?,?),(?,?)}。
     *
     * @param tableName 表名
     * @param columns   列名
     * @param rows      行数
     * @return SQL 文本
     */
    public static String insertSql(String tableName, List<String> columns, int rows) {
        if (Strings.isBlank(tableName)) {
            throw new BanyanException(ErrorCode.TABLE_NAME_REQUIRED);
        }
        if (CollUtils.isEmpty(columns)) {
            throw new BanyanException(ErrorCode.COLUMNS_REQUIRED);
        }
        if (rows <= 0) {
            throw new BanyanException(ErrorCode.ROWS_POSITIVE);
        }
        String columnText = String.join(",", columns);
        String rowText = "(" + String.join(",", Collections.nCopies(columns.size(), "?")) + ")";
        return "INSERT INTO " + tableName + " (" + columnText + ") VALUES "
                + String.join(",", Collections.nCopies(rows, rowText));
    }

    /**
     * 构造 CASE WHEN 形式的单条批量 UPDATE：
     * {@code UPDATE t SET c = CASE id WHEN ? THEN ? ... ELSE c END, ... WHERE id IN (?,...)}。
     * <p>每列只为本批中该列非 null 的行生成 WHEN 分支，未生成分支的行由 {@code ELSE c} 保留原值；
     * 占位符顺序为「列 → 行」逐列排列，最后由主键参数填充 WHERE。</p>
     *
     * @param tableName   表名
     * @param idColumn    主键列名
     * @param setColumns  需要更新的列（本批非 null 字段并集，不含主键）
     * @param valueCounts 与 {@code setColumns} 一一对应，每列非 null 值的行数
     * @param rows        本次更新的行数
     * @return SQL 文本
     */
    public static String updateCaseWhenSql(String tableName, String idColumn, List<String> setColumns,
                                           List<Integer> valueCounts, int rows) {
        if (Strings.isBlank(tableName)) {
            throw new BanyanException(ErrorCode.TABLE_NAME_REQUIRED);
        }
        if (Strings.isBlank(idColumn)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.ID_COLUMN_REQUIRED));
        }
        if (CollUtils.isEmpty(setColumns)) {
            throw new BanyanException(ErrorCode.COLUMNS_REQUIRED);
        }
        if (rows <= 0) {
            throw new BanyanException(ErrorCode.ROWS_POSITIVE);
        }
        if (setColumns.size() != valueCounts.size()) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.COLUMN_VALUE_COUNT_MISMATCH));
        }
        StringBuilder sql = new StringBuilder(
                UPDATE_SQL_INITIAL_CAPACITY + setColumns.size() * rows * UPDATE_SQL_PER_BRANCH);
        sql.append("UPDATE ").append(tableName).append(" SET ");
        for (int i = 0; i < setColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            String column = setColumns.get(i);
            sql.append(column).append(" = CASE ").append(idColumn);
            // 只为本批该列非 null 的行生成分支；未生成分支的行由 ELSE 保留原值
            for (int branch = 0; branch < valueCounts.get(i); branch++) {
                sql.append(" WHEN ? THEN ?");
            }
            sql.append(" ELSE ").append(column).append(" END");
        }
        sql.append(" WHERE ").append(idColumn).append(" IN (")
                .append(String.join(",", Collections.nCopies(rows, "?"))).append(")");
        return sql.toString();
    }
}
