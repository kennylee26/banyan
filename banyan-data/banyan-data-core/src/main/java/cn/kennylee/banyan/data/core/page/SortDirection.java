package cn.kennylee.banyan.data.core.page;

/**
 * 排序方向。
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum SortDirection {

    /** 升序。 */
    ASC,

    /** 降序。 */
    DESC;

    /**
     * 按文本解析排序方向，忽略大小写；无法识别时按升序处理。
     *
     * @param value 方向文本
     * @return 排序方向
     */
    public static SortDirection from(String value) {
        if (value == null) {
            return ASC;
        }
        return DESC.name().equalsIgnoreCase(value.trim()) ? DESC : ASC;
    }
}
