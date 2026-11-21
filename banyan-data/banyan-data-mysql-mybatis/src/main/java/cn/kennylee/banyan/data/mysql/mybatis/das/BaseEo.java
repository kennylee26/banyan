package cn.kennylee.banyan.data.mysql.mybatis.das;

import cn.kennylee.banyan.data.core.das.Identifiable;
import java.io.Serializable;

/**
 * 实体基类：只声明主键契约，不声明 {@code id} 字段。
 * <p>主键字段由使用方在自己的实体上声明，并按需加 {@code @TableId}。无注解时 MyBatis-Plus
 * 会把名为 {@code id} 的字段自动识别为主键、采用全局 {@code DbConfig.idType}（默认 ASSIGN_ID 雪花）；
 * 要改成 AUTO / ASSIGN_UUID / INPUT，直接在自己的字段上写 {@code @TableId(type = ...)} 即可。</p>
 * <p>不在基类声明 id 字段，是为了避免子类重声明同名字段时 MP 收集出两个 id 属性。</p>
 *
 * @param <P> 主键类型
 * @author kennylee
 * @since 0.1.0
 */
public abstract class BaseEo<P extends Serializable> implements Identifiable<P> {
}
