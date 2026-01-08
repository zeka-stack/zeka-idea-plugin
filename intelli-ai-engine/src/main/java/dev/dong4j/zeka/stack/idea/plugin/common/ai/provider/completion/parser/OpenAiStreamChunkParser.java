package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

/**
 * OpenAI 流解析器内部类
 * <p> 实现了一个用于解析 OpenAI API 返回的流数据的解析器
 * <p> 该类继承自 StreamChunkParser 接口, 负责将 JSON 对象解析为 StreamChunk 对象
 * <p> 具体解析逻辑如下:
 * <ul>
 * <li> 读取 JSON 对象中的第一个 delta 字段 </li>
 * <li> 从 delta 中提取 content 字符串 </li>
 * <li> 如果 content 存在且不为空, 则返回一个新的 StreamChunk 对象, 否则返回 null</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class OpenAiStreamChunkParser implements StreamChunkParser {
    /**
     * 解析 OpenAI 流式响应中的 JSON 数据块
     * <p> 从流式响应的 JSON 数据中提取 delta 内容, 并将其封装为 StreamChunk 对象返回
     * <p> 该方法会读取 JSON 中的第一个 delta 节点, 如果 delta 存在且包含非空的 content 字段,
     * 则返回包含该内容的 StreamChunk; 否则返回 null
     *
     * @param json 流式响应的 JSON 数据, 不能为 null
     * @return StreamChunk 对象, 包含解析出的内容; 如果 delta 不存在或 content 为空则返回 null
     */
    @Override
    public StreamChunk parse(@NotNull JsonObject json) {
        JsonObject delta = StreamChunkParser.readFirstDelta(json);
        if (delta == null) {
            return null;
        }
        String content = StreamChunkParser.readStringValue(delta, "content");
        if (content == null || content.isEmpty()) {
            return null;
        }
        return new StreamChunk(content, null);
    }
}
