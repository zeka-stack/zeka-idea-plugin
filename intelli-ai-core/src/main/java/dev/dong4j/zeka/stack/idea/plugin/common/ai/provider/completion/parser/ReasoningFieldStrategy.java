package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 思考字段策略类
 * <p> 用于处理包含思考内容的流数据解析, 主要负责识别并提取数据中的思考字段信息, 如 reasoning 和 reasoningContent, 并将其转换为相应的 StreamChunk 类型进行输出.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class ReasoningFieldStrategy implements StreamParseStrategy {
    /**
     * 获取当前策略的优先级
     * <p> 该方法用于定义此解析策略在处理流数据时的优先级数值, 数值越大优先级越高
     *
     * @return 优先级值, 当前固定返回 100
     */
    @Override
    public int priority() {
        return 100;
    }

    /**
     * 判断是否支持当前解析上下文和流块数据
     * <p> 当流块包含推理文本或推理内容时返回 true
     *
     * @param context 解析上下文, 不能为 null
     * @param chunk   流块数据, 不能为 null
     * @return 如果流块包含推理文本或推理内容则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        return hasText(chunk.reasoning()) || hasText(chunk.reasoningContent());
    }

    /**
     * 解析原始流数据并生成对应的流块
     * <p> 根据给定的上下文和流块内容, 解析出思考内容和正文内容, 并通过发射器发送相应的流块.
     *
     * @param context 解析上下文, 用于提供解析所需的环境信息
     * @param chunk   原始流块数据, 包含需要解析的内容
     * @param emitter 流块发射器, 用于将解析后的流块发送出去
     */
    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        emitIfPresent(emitter, StreamChunkType.THINKING, chunk.reasoningContent());
        emitIfPresent(emitter, StreamChunkType.THINKING, chunk.reasoning());
        emitIfPresent(emitter, StreamChunkType.CONTENT, chunk.content());
    }

    /**
     * 如果文本存在且非空, 则向流块发射器发出流块
     * <p> 检查给定的文本是否为空或 null, 如果不为空, 则创建一个新的流块并将其发射到流块发射器中
     *
     * @param emitter 流块发射器, 用于发射流块
     * @param type    流块类型, 指定发射的流块类型
     * @param text    要检查和发射的文本, 可以为 null 或空字符串
     */
    private void emitIfPresent(StreamChunkEmitter emitter, StreamChunkType type, @Nullable String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        emitter.emit(new StreamChunk(type, text));
    }

    /**
     * 判断字符串是否包含有效文本
     * <p> 检查字符串是否不为 null 且长度大于 0, 用于判断文本是否有效
     *
     * @param text 待检查的字符串, 可能为 null
     * @return 如果字符串不为 null 且非空, 则返回 true, 否则返回 false
     */
    private boolean hasText(@Nullable String text) {
        return text != null && !text.isEmpty();
    }
}
