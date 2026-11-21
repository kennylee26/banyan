package cn.kennylee.banyan.data.mysql.mybatis.das;

/**
 * 主键生成策略。由 {@link BaseDas} 统一持有，同时作用于 MyBatis-Plus 的插入与手写多值 INSERT。
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum IdPolicy {

    /** 数据库自增：不补 ID，由数据库生成并回填。 */
    AUTO,

    /** 分配 ID：默认雪花算法，可通过 IdProvider 替换。 */
    ASSIGN_ID,

    /** 分配 UUID：字符串主键。 */
    ASSIGN_UUID,

    /** 使用方自己设置 ID：框架不补。 */
    INPUT
}
