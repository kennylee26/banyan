package cn.kennylee.banyan.data.core.page;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 排序项：数据库字段名 + 方向。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@RequiredArgsConstructor(staticName = "of")
public class SortDto {

    /** 升序。 */
    public static final String ASC = "asc";

    /** 降序。 */
    public static final String DESC = "desc";

    /** 字段名。 */
    private final String field;

    /** 排序方向。 */
    private final SortDirection direction;

    /**
     * 按文本方向构造排序项，兼容 {@code "asc"} / {@code "desc"}。
     *
     * @param field     字段名
     * @param direction 方向文本
     * @return 排序项
     */
    public static SortDto of(String field, String direction) {
        return new SortDto(field, SortDirection.from(direction));
    }
}
