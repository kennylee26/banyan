package cn.kennylee.banyan.excel.parser;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

/**
 * sheet 选择：按名称或索引二选一。
 *
 * <p>名称与索引不能同时为空或同时存在；默认读取第一个 sheet。</p>
 *
 * @param name  sheet 名称，按索引选择时为 {@code null}
 * @param index sheet 索引，按名称选择时为 {@code null}
 * @author kennylee
 * @since 0.1.0
 */
public record SheetRef(String name, Integer index) {

    /**
     * 校验名称与索引恰好设置一个。
     */
    public SheetRef {
        if ((name == null) == (index == null)) {
            throw new BanyanException(ErrorCode.PARSE_CONFIG_INVALID, "sheet");
        }
        if (index != null && index < 0) {
            throw new BanyanException(ErrorCode.PARSE_CONFIG_INVALID, "sheet.index");
        }
    }

    /**
     * 按索引选择 sheet。
     *
     * @param index 从 0 开始的 sheet 索引
     * @return sheet 选择
     * @throws BanyanException 索引为负数
     */
    public static SheetRef ofIndex(int index) {
        return new SheetRef(null, index);
    }

    /**
     * 按名称选择 sheet。
     *
     * @param name sheet 名称
     * @return sheet 选择
     * @throws BanyanException 名称为空
     */
    public static SheetRef ofName(String name) {
        return new SheetRef(name, null);
    }
}
