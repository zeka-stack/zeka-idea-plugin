package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * 构建 OpenAI 兼容请求体时对 {@code enable_thinking} 的发送策略
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ThinkingSendMode {
    /** 不写入该字段 */
    OMIT,
    /** 写入 enable_thinking: true */
    TRUE,
    /** 写入 enable_thinking: false (当前默认策略不使用, 预留) */
    FALSE
}
