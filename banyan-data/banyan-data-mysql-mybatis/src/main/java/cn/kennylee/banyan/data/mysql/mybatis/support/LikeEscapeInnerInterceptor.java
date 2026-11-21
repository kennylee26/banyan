package cn.kennylee.banyan.data.mysql.mybatis.support;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 全局 LIKE 特殊字符转义拦截器：在 SQL 执行前把 like 位置的字符串参数转义。
 * <p>与策略层是<b>分工</b>而非互斥：策略层负责查询 DTO 产出的 wrapper 参数；本拦截器负责手写
 * SQL 的顶层参数，并在策略层关闭时兜底处理 wrapper 参数。全局层开启且策略层也开启时，wrapper
 * 参数由策略层转义，本拦截器不再重复处理，从而保证同一原始值只转一次。</p>
 * <p>覆盖两种可识别形态：顶层 Map 参数，以及 {@code ew.paramNameValuePairs.*} 参数。使用方自行
 * 拼接首尾通配符时保留首尾 {@code %}；嵌套 bean 等无法识别的形态原样放过并输出 DEBUG，不抛异常。</p>
 * <p>分页会为同一次调用执行 count 与数据两条 SQL，二者可能共用参数对象；参数表中会写入内部
 * 标记，保证同一次调用只转义一次。该标记不是业务参数，MyBatis 只读取被引用的键，不影响绑定。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class LikeEscapeInnerInterceptor implements InnerInterceptor {

    /** LIKE 关键字匹配。 */
    private static final Pattern LIKE_PATTERN = Pattern.compile("(?i)\\blike\\b");

    /** 条件构造器参数前缀。 */
    private static final String WRAPPER_PARAM_PREFIX = "ew.paramNameValuePairs.";

    /** 条件构造器在参数 Map 中的键。 */
    private static final String WRAPPER_KEY = "ew";

    /** 通配符字符。 */
    private static final char WILDCARD = '%';

    /** 参数分隔符。 */
    private static final String PARAMETER_PLACEHOLDER = "\\?";

    /** 属性路径分隔符。 */
    private static final String PROPERTY_SEPARATOR = ".";

    /** 已处理标记。 */
    private static final String ESCAPED_MARKER = "__banyan_like_escaped__";

    /** 策略层是否启用转义；启用时 wrapper 参数已由策略层处理，本拦截器不再重复转义。 */
    private final boolean strategyEscapeEnabled;

    /**
     * 构造实例，默认策略层已启用转义。
     */
    public LikeEscapeInnerInterceptor() {
        this(true);
    }

    /**
     * 构造实例。
     *
     * @param strategyEscapeEnabled 策略层是否启用转义
     */
    public LikeEscapeInnerInterceptor(boolean strategyEscapeEnabled) {
        this.strategyEscapeEnabled = strategyEscapeEnabled;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        if (!(parameter instanceof Map<?, ?> parameterMap)) {
            log.debug(Messages.get(ErrorCode.LIKE_ESCAPE_UNSUPPORTED_PARAM,
                    parameter == null ? "null" : parameter.getClass().getName()));
            return;
        }
        if (parameterMap.containsKey(ESCAPED_MARKER)) {
            return;
        }
        String sql = boundSql.getSql();
        if (sql == null || !LIKE_PATTERN.matcher(sql).find()) {
            return;
        }
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        String[] segments = sql.split(PARAMETER_PLACEHOLDER, -1);
        List<AbstractWrapper<?, ?, ?>> escapedWrappers = new ArrayList<>();
        for (int i = 0; i < mappings.size() && i < segments.length; i++) {
            if (!LIKE_PATTERN.matcher(segments[i]).find()) {
                continue;
            }
            String property = mappings.get(i).getProperty();
            if (Strings.isNotBlank(property)) {
                AbstractWrapper<?, ?, ?> wrapper = escapeParameter(parameterMap, property);
                if (wrapper != null) {
                    escapedWrappers.add(wrapper);
                }
            }
        }
        markEscaped(parameterMap);
        // 同一 Wrapper 的多个 like 参数全部处理完再打标记，避免只转第一个
        escapedWrappers.forEach(wrapper -> wrapper.getParamNameValuePairs().put(ESCAPED_MARKER, Boolean.TRUE));
    }

    /**
     * 按参数形态转义 like 值。
     *
     * @param parameterMap 参数对象
     * @param property     参数属性名
     * @return 被本次转义过的条件构造器；不是 wrapper 参数或已处理时返回 null
     */
    @SuppressWarnings("unchecked")
    private AbstractWrapper<?, ?, ?> escapeParameter(Map<?, ?> parameterMap, String property) {
        Map<String, Object> parameters = (Map<String, Object>) parameterMap;
        if (property.startsWith(WRAPPER_PARAM_PREFIX)) {
            return escapeWrapperParameter(parameters, property);
        }
        if (property.contains(PROPERTY_SEPARATOR)) {
            log.debug(Messages.get(ErrorCode.LIKE_ESCAPE_UNSUPPORTED_PARAM, property));
            return null;
        }
        Object value = parameters.get(property);
        if (value instanceof String text) {
            // 顶层参数是使用方的原始数据，整体转义；通配符应由 SQL 自行拼接（如 CONCAT）
            parameters.put(property, LikeEscaper.escape(text));
        }
        return null;
    }

    /**
     * 转义条件构造器参数表中的值；策略层已启用时不重复处理。
     *
     * @param parameters 参数对象
     * @param property   参数属性名
     * @return 被转义的条件构造器；跳过时返回 null
     */
    private AbstractWrapper<?, ?, ?> escapeWrapperParameter(Map<String, Object> parameters, String property) {
        if (strategyEscapeEnabled) {
            return null;
        }
        Object wrapper = parameters.get(WRAPPER_KEY);
        if (!(wrapper instanceof AbstractWrapper<?, ?, ?> abstractWrapper)) {
            return null;
        }
        Map<String, Object> values = abstractWrapper.getParamNameValuePairs();
        if (values.containsKey(ESCAPED_MARKER)) {
            return null;
        }
        String key = property.substring(WRAPPER_PARAM_PREFIX.length());
        Object value = values.get(key);
        if (value instanceof String text) {
            // Wrapper 参数的首尾 % 由 MyBatis-Plus 或 likeLeft/likeRight 添加，需要保留
            values.put(key, escapeWrapperValue(text));
        }
        return abstractWrapper;
    }

    /**
     * 保留 Wrapper 值的首尾通配符（含单侧），只转义中间内容。
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private String escapeWrapperValue(String value) {
        if (value.isEmpty()) {
            return value;
        }
        boolean leading = value.charAt(0) == WILDCARD;
        boolean trailing = value.length() > 1 && value.charAt(value.length() - 1) == WILDCARD;
        int start = leading ? 1 : 0;
        int end = trailing ? value.length() - 1 : value.length();
        if (start > end) {
            start = end;
        }
        return (leading ? String.valueOf(WILDCARD) : "")
                + LikeEscaper.escape(value.substring(start, end))
                + (trailing ? String.valueOf(WILDCARD) : "");
    }

    /**
     * 标记该参数对象已处理。
     *
     * @param parameterMap 参数对象
     */
    @SuppressWarnings("unchecked")
    private void markEscaped(Map<?, ?> parameterMap) {
        ((Map<String, Object>) parameterMap).put(ESCAPED_MARKER, Boolean.TRUE);
    }
}
