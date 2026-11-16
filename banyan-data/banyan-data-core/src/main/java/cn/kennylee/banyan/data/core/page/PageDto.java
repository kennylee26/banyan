package cn.kennylee.banyan.data.core.page;

import cn.kennylee.banyan.data.core.validation.DynamicMaxPage;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 分页入参。{@code searchCount} 三态语义保持不变：null = 同时查总数与数据；true = 仅查总数；false = 仅查数据。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Data
@DynamicMaxPage
public class PageDto {

    /** 分页相关字段名，查询策略需从条件中排除。 */
    public static final Set<String> PROPERTY_NAMES = Set.of("pageNo", "pageSize", "searchCount", "sorts");

    /** 默认页码。 */
    public static final int DEFAULT_PAGE_NO = 1;

    /** 默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 页码，默认 1。 */
    private Integer pageNo = DEFAULT_PAGE_NO;

    /** 每页条数，默认 10。 */
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /** 是否查询总数：null 同时查总数与数据，true 仅查总数，false 仅查数据。 */
    private Boolean searchCount;

    /** 排序项列表。 */
    private List<SortDto> sorts;
}
