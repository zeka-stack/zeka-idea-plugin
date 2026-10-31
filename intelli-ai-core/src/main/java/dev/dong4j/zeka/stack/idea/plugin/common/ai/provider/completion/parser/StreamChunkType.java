package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

/**
 * 流式数据块类型枚举
 * <p> 用于标识流式传输中不同类型的数据块, 常用于 AI 交互, 工具调用等场景下的分段处理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public enum StreamChunkType {
    /** 流式块类型, 表示当前处理阶段为思考状态 */
    THINKING,
    /** 内容块类型 */
    CONTENT,
    /** 工具调用类型的枚举值, 表示流式块中与工具交互的部分. */
    TOOL_CALL,
    /** 语义化流式块类型中的元数据标记 */
    META,
    /** 提示信息类型, 用于向 UI 发送提示文本 */
    NOTICE,
    /** 流式块结束标记 */
    END
}
