package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;

/**
 * 流式数据块记录类
 * <p> 用于表示流式传输中的数据块, 包含数据类型和文本内容两个核心属性
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public record StreamChunk(@NotNull StreamChunkType type, @NotNull String text) {
}
