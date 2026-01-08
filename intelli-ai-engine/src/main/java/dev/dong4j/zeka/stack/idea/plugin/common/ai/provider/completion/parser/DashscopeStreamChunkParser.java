package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

/**
 * Dashscope 流式响应块解析器
 * <p>用于解析 Dashscope API 返回的流式响应数据, 提取内容字段 (content) 和思考内容字段(reasoning_content), 并封装为 StreamChunk 对象.
 * <p>该解析器适用于处理带有增量内容的流式响应, 当内容或思考内容为空时, 返回 null 表示无有效数据.
 * <p>使用示例:
 * <pre>{@code
 * DashscopeStreamChunkParser parser = new DashscopeStreamChunkParser();
 * StreamChunk chunk = parser.parse(json);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class DashscopeStreamChunkParser implements StreamChunkParser {
    /**
     * 解析 JSON 数据并返回对应的流块对象
     * <p> 该方法从指定的 JSON 对象中读取 "delta" 字段, 并从中提取 "content" 和 "reasoning_content" 的值.
     * 如果这两个字段都为空或未提供, 则返回 null. 否则, 使用这些值构造一个新的 StreamChunk 对象并返回.
     *
     * @param json 包含 delta 信息的 JSON 对象, 不能为 null
     * @return 解析后的 StreamChunk 对象, 如果 content 和 reasoning_content 都为空则返回 null
     */
    @Override
    public StreamChunk parse(@NotNull JsonObject json) {
        JsonObject delta = StreamChunkParser.readFirstDelta(json);
        if (delta == null) {
            return null;
        }
        String content = StreamChunkParser.readStringValue(delta, "content");
        String thinking = StreamChunkParser.readStringValue(delta, "reasoning_content");
        if ((content == null || content.isEmpty()) && (thinking == null || thinking.isEmpty())) {
            return null;
        }
        return new StreamChunk(content, thinking);
    }
}
