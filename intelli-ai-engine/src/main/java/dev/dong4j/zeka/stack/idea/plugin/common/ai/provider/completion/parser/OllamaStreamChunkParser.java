package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

/**
 * Ollama 流式数据解析器
 * <p> 用于解析 Ollama 模型返回的流式数据, 提取内容和推理信息
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class OllamaStreamChunkParser implements StreamChunkParser {
    /**
     * 解析 JSON 数据为 StreamChunk 对象
     * <p> 该方法接收一个 JsonObject 参数, 解析其中的 delta 数据, 提取 content 和 thinking 字段, 若两者均为空则返回 null, 否则创建并返回 StreamChunk 实例.
     *
     * @param json 输入的 JSON 对象, 不能为 null
     * @return 解析后的 StreamChunk 实例, 若内容为空则返回 null
     */
    @Override
    public StreamChunk parse(@NotNull JsonObject json) {
        JsonObject delta = StreamChunkParser.readFirstDelta(json);
        if (delta == null) {
            return null;
        }
        String content = StreamChunkParser.readStringValue(delta, "content");
        String thinking = StreamChunkParser.readStringValue(delta, "reasoning");
        if ((content == null || content.isEmpty()) && (thinking == null || thinking.isEmpty())) {
            return null;
        }
        return new StreamChunk(content, thinking);
    }
}
