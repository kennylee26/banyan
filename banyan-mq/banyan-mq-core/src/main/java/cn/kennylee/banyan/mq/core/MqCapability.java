package cn.kennylee.banyan.mq.core;

/**
 * 后端可选能力。
 *
 * <p>各后端声明自身支持的能力集合；业务使用到不支持的能力时启动期直接报错，
 * 不允许静默忽略。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum MqCapability {

    /** 单个 tag 过滤。 */
    TAG,

    /** 多个 tag 过滤（后端需支持 tag 表达式）。 */
    MULTI_TAG,

    /** 延迟消息。 */
    DELAY
}
