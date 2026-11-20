package cn.kennylee.banyan.commons.util;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean 工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class BeansTest {

    /** Bean 转 Map 只保留非空属性。 */
    @Test
    void toMapKeepsOnlyNonNullProperties() {
        // given
        User bean = new User("alice", 20, null);

        // when
        Map<String, Object> map = Beans.toMap(bean, Collections.emptyList());

        // then
        assertEquals("alice", map.get("name"));
        assertEquals(20, map.get("age"));
        assertFalse(map.containsKey("email"));
    }

    /** Bean 转 Map 跳过忽略属性。 */
    @Test
    void toMapSkipsIgnoredProperties() {
        // given
        User bean = new User("alice", 20, "a@b.com");

        // when
        Map<String, Object> map = Beans.toMap(bean, Collections.singletonList("age"));

        // then
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("email"));
    }

    /** getter 方法名解析属性名；非 getter 原样返回。 */
    @Test
    void parsesPropertyNames() {
        // when
        assertEquals("name", Beans.propertyName("getName"));
        assertEquals("active", Beans.propertyName("isActive"));
        assertEquals("URL", Beans.propertyName("getURL"));
        assertEquals("count", Beans.propertyName("count"));
    }

    /** 首字母转小写：前两个字母都大写时保持原样。 */
    @Test
    void decapitalizesLikeJavaBeans() {
        // when
        assertEquals("name", Beans.decapitalize("Name"));
        assertEquals("URL", Beans.decapitalize("URL"));
        assertEquals("", Beans.decapitalize(""));
    }

    /** 对 null 返回空 Map。 */
    @Test
    void toMapOnNullReturnsEmptyMap() {
        // when
        assertTrue(Beans.toMap(null, Collections.emptyList()).isEmpty());
    }

    /** 忽略属性为 null 时等同于不忽略任何属性。 */
    @Test
    void toMapOnNullIgnoresKeepsAllProperties() {
        // when
        Map<String, Object> map = Beans.toMap(new User("alice", 20, "a@b.com"), null);

        // then
        assertEquals("alice", map.get("name"));
        assertEquals(20, map.get("age"));
        assertEquals("a@b.com", map.get("email"));
    }

    /** 布尔 getter 参与转换，getClass 与非 getter 方法被排除。 */
    @Test
    void toMapIncludesBooleanGetterAndExcludesNonGetters() {
        // when
        Map<String, Object> map = Beans.toMap(new Account(true), Collections.emptyList());

        // then
        assertEquals(true, map.get("active"));
        assertEquals("fixed", map.get("label"));
        assertFalse(map.containsKey("class"));
        assertFalse(map.containsKey("compute"));
        assertFalse(map.containsKey("nothing"));
    }

    /** 非布尔的 is 前缀方法不是 getter。 */
    @Test
    void toMapSkipsNonBooleanIsMethod() {
        // when
        Map<String, Object> map = Beans.toMap(new Account(true), Collections.emptyList());

        // then
        assertFalse(map.containsKey("name"));
    }

    /** getter 抛异常时包装成带错误码与属性名的领域异常。 */
    @Test
    void toMapWrapsGetterFailure() {
        // when
        BanyanException exception = assertThrows(BanyanException.class,
                () -> Beans.toMap(new FailingBean(), Collections.emptyList()));

        // then
        assertEquals(ErrorCode.BEAN_READ_FAILED, exception.getErrorCode());
        assertInstanceOf(InvocationTargetException.class, exception.getCause());
    }

    /** 有参方法即使有返回值也不是 getter。 */
    @Test
    void toMapSkipsParameterizedMethods() {
        // when
        Map<String, Object> map = Beans.toMap(new Account(true), Collections.emptyList());

        // then
        assertFalse(map.containsKey("withArg"));
    }

    /** 单字符名称首字母转小写，不越界。 */
    @Test
    void decapitalizesSingleCharacter() {
        // when
        assertEquals("a", Beans.decapitalize("A"));
    }

    /** 首字母小写、第二字母大写时保持首字母小写。 */
    @Test
    void decapitalizesMixedCaseFirstLetter() {
        // when
        assertEquals("aB", Beans.decapitalize("aB"));
    }

    static class Account {

        private final boolean active;

        Account(boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }

        public String getLabel() {
            return "fixed";
        }

        public String isName() {
            return "not a getter";
        }

        public int compute() {
            return 1;
        }

        public String getWithArg(String prefix) {
            return prefix;
        }

        public void nothing() {
        }
    }

    static class FailingBean {

        public String getBroken() {
            throw new IllegalStateException("boom");
        }
    }

    static class User {
        private final String name;
        private final Integer age;
        private final String email;

        User(String name, Integer age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public String getEmail() {
            return email;
        }
    }
}
