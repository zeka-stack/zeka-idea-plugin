package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.Nullable;

/**
 * 流式数据块记录类
 * <p> 用于表示流式传输中的一个数据片段, 通常用于异步通信或分段处理场景中.
 * <p> 该记录类包含两个字段:content 表示实际内容,thinking 表示附加的思考信息 (如 AI 推理过程).
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public record StreamChunk(@Nullable String content, @Nullable String thinking) {
}
