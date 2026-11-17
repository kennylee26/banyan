package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        User bean = new User("alice", 20, null);

        Map<String, Object> map = Beans.toMap(bean, Collections.emptyList());

        assertEquals("alice", map.get("name"));
        assertEquals(20, map.get("age"));
        assertFalse(map.containsKey("email"));
    }

    /** Bean 转 Map 跳过忽略属性。 */
    @Test
    void toMapSkipsIgnoredProperties() {
        User bean = new User("alice", 20, "a@b.com");

        Map<String, Object> map = Beans.toMap(bean, Collections.singletonList("age"));

        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("email"));
    }

    /** getter 方法名解析属性名；非 getter 原样返回。 */
    @Test
    void parsesPropertyNames() {
        assertEquals("name", Beans.propertyName("getName"));
        assertEquals("active", Beans.propertyName("isActive"));
        assertEquals("URL", Beans.propertyName("getURL"));
        assertEquals("count", Beans.propertyName("count"));
    }

    /** 首字母转小写：前两个字母都大写时保持原样。 */
    @Test
    void decapitalizesLikeJavaBeans() {
        assertEquals("name", Beans.decapitalize("Name"));
        assertEquals("URL", Beans.decapitalize("URL"));
        assertEquals("", Beans.decapitalize(""));
    }

    /** 对 null 返回空 Map。 */
    @Test
    void toMapOnNullReturnsEmptyMap() {
        assertTrue(Beans.toMap(null, Collections.emptyList()).isEmpty());
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
