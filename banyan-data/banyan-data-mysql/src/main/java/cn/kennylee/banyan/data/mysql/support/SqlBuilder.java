package cn.kennylee.banyan.data.mysql.support;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.util.CollUtils;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.commons.error.ErrorCode;
import java.util.Collections;
import java.util.List;

/**
 * SQL 文本构造工具。只负责拼 SQL，不执行、不持有连接。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class SqlBuilder {

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
}
