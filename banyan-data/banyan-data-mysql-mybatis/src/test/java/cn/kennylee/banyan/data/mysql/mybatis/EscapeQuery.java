package cn.kennylee.banyan.data.mysql.mybatis;

import cn.kennylee.banyan.data.core.page.PageDto;
import lombok.Getter;
import lombok.Setter;

/**
 * LIKE 转义测试共用的查询入参。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
class EscapeQuery extends PageDto {

    /** 全模糊条件。 */
    private String userNameLike;

    /** 前模糊条件。 */
    private String userNamePreLike;
}
