package cn.kennylee.banyan.data.core.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询操作符解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class CondOperationTest {

    /** 默认按相等处理。 */
    @Test
    void defaultIsEq() {
        // when
        assertEquals(CondOperation.EQ, CondOperation.match("userName"));
    }

    /** 每个后缀都能匹配。 */
    @Test
    void matchesEachSuffix() {
        // when
        assertEquals(CondOperation.EQ, CondOperation.match("userNameEq"));
        assertEquals(CondOperation.NE, CondOperation.match("userNameNe"));
        assertEquals(CondOperation.GT, CondOperation.match("ageGt"));
        assertEquals(CondOperation.GE, CondOperation.match("ageGe"));
        assertEquals(CondOperation.LT, CondOperation.match("ageLt"));
        assertEquals(CondOperation.LE, CondOperation.match("ageLe"));
        assertEquals(CondOperation.LIKE, CondOperation.match("userNameLike"));
        assertEquals(CondOperation.PRE_LIKE, CondOperation.match("userNamePreLike"));
        assertEquals(CondOperation.SUF_LIKE, CondOperation.match("userNameSufLike"));
        assertEquals(CondOperation.NOT_LIKE, CondOperation.match("userNameNotLike"));
        assertEquals(CondOperation.IN, CondOperation.match("statusIn"));
        assertEquals(CondOperation.NOT_IN, CondOperation.match("statusNotIn"));
        assertEquals(CondOperation.IS_NULL, CondOperation.match("emailIsNull"));
        assertEquals(CondOperation.IS_NOT_NULL, CondOperation.match("emailIsNotNull"));
    }

    /** 后缀互斥。 */
    @Test
    void suffixesAreMutuallyExclusive() {
        // when
        assertEquals(CondOperation.NOT_LIKE, CondOperation.match("nameNotLike"));
        assertEquals(CondOperation.NOT_IN, CondOperation.match("statusNotIn"));
        assertEquals(CondOperation.PRE_LIKE, CondOperation.match("namePreLike"));
        assertEquals(CondOperation.SUF_LIKE, CondOperation.match("nameSufLike"));
    }

    /** 后缀匹配忽略大小写。 */
    @Test
    void matchesSuffixIgnoringCase() {
        // when
        assertTrue(CondOperation.LIKE.isEndWithIgnoreCase("userNameLIKE"));
        assertTrue(CondOperation.NOT_IN.isEndWithIgnoreCase("statusnotin"));
        assertFalse(CondOperation.LIKE.isEndWithIgnoreCase("userName"));
        assertFalse(CondOperation.EQ.isEndWithIgnoreCase("userName"));
    }

    /** 截取后缀得到驼峰属性名。 */
    @Test
    void stripsSuffixInCamelCase() {
        // when
        assertEquals("userName", CondOperation.LIKE.subFieldNameCamelCase("userNameLike"));
        assertEquals("userName", CondOperation.EQ.subFieldNameCamelCase("userName"));
        assertEquals("status", CondOperation.NOT_IN.subFieldNameCamelCase("statusNotIn"));
    }

    /** 截取后缀得到下划线列名。 */
    @Test
    void stripsSuffixInUnderlineCase() {
        // when
        assertEquals("user_name", CondOperation.LIKE.subFieldNameUnderlineCase("userNameLike"));
        assertEquals("user_name", CondOperation.EQ.subFieldNameUnderlineCase("userName"));
    }

    /** 范围字段识别。 */
    @Test
    void rangeFieldsAreDetected() {
        // when
        assertTrue(CondOperation.isRangeField("ageGt"));
        assertTrue(CondOperation.isRangeField("ageLe"));
        assertFalse(CondOperation.isRangeField("nameLike"));
        assertFalse(CondOperation.isRangeField("name"));
    }
}
