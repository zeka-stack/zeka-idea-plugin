package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

/**
 * MiniMax 流式分块解析器
 * <p>用于解析 MiniMax API 返回的流式响应内容, 识别并分离思考过程 (think) 与最终答案 (answer) 部分.
 * <p>支持通过特定起始符 {@code THINK_START} 和结束符 {@code THINK_END} 来标记思考内容区域.
 * <p>解析结果封装为 {@code StreamChunk} 对象, 包含思考文本和答案文本两个字段.
 * <p>使用示例:
 * <pre>{@code
 * MiniMaxStreamChunkParser parser = new MiniMaxStreamChunkParser();
 * StreamChunk chunk = parser.parse(json);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class MiniMaxStreamChunkParser implements StreamChunkParser {
    /**
     * 表示思考模式开始的标记
     * <p> 在解析内容时, 遇到此标记表示进入思考模式
     */
    private static final String THINK_START = "<think>";
    /**  */
    private static final String THINK_END = "</think>";
    private boolean inThinking;

    /**
     * 解析 JSON 数据流中的内容, 提取思考部分和回答部分
     * <p> 该方法用于解析包含思考标记和回答标记的 JSON 数据流, 将内容分割为思考文本和回答文本, 并返回一个 StreamChunk 对象.
     *
     * @param json 包含数据流的 JSON 对象, 不能为 null
     * @return 包含解析后的回答文本和思考文本的 StreamChunk 对象, 如果两者都为空则返回 null
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
        StringBuilder thinking = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        int index = 0;
        while (index < content.length()) {
            if (inThinking) {
                int endIndex = content.indexOf(THINK_END, index);
                if (endIndex == -1) {
                    thinking.append(content.substring(index));
                    index = content.length();
                    continue;
                }
                thinking.append(content, index, endIndex);
                index = endIndex + THINK_END.length();
                inThinking = false;
                continue;
            }
            int startIndex = content.indexOf(THINK_START, index);
            if (startIndex == -1) {
                answer.append(content.substring(index));
                index = content.length();
                continue;
            }
            answer.append(content, index, startIndex);
            index = startIndex + THINK_START.length();
            inThinking = true;
        }
        String thinkingText = !thinking.isEmpty() ? thinking.toString() : null;
        String answerText = !answer.isEmpty() ? answer.toString() : null;
        if (thinkingText == null && answerText == null) {
            return null;
        }
        return new StreamChunk(answerText, thinkingText);
    }
}
