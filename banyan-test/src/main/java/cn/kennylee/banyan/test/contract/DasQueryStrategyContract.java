package cn.kennylee.banyan.test.contract;

import cn.kennylee.banyan.commons.error.BanyanException;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 查询策略的抽象契约用例。各数据库实现继承本类并实现 {@link #parse(Map)} 即可跑同一套断言。
 *
 * @author kennylee
 * @since 0.1.0
 */
public abstract class DasQueryStrategyContract {

    /**
     * 用被测策略解析查询入参。
     *
     * @param query 查询入参
     * @return 具体数据库的查询容器
     */
    protected abstract Object parse(Map<String, Object> query);

    /** 分页字段不应参与查询条件。 */
    @Test
    void skipsPagingFields() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("userNameLike", "ali");
        query.put("pageNo", 2);
        query.put("pageSize", 20);

        assertDoesNotThrow(() -> parse(query));
    }

    /** 实体上不存在的属性应报错。 */
    @Test
    void rejectsUnknownProperty() {
        assertThrows(BanyanException.class, () -> parse(Map.of("unknownLike", "x")));
    }
}
