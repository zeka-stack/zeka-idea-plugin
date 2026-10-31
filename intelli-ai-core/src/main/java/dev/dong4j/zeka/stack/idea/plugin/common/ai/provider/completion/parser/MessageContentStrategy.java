package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;

/**
 * 消息内容解析策略类
 * <p> 实现 {@link StreamParseStrategy} 接口, 用于识别和处理原始流中的消息内容数据块.
 * <p> 该策略通过检查数据块的内容字段是否非空来判断是否支持当前数据块的解析, 并将有效内容封装为 CONTENT 类型的数据块进行发射.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class MessageContentStrategy implements StreamParseStrategy {
    /**
     * 获取当前策略的优先级
     * <p> 该方法返回此消息内容解析策略的执行优先级数值, 数值越大表示优先级越高
     *
     * @return 优先级值, 当前固定返回 50
     * @since 1.0.0
     */
    @Override
    public int priority() {
        return 50;
    }

    /**
     * 判断当前解析策略是否支持给定的解析上下文和原始流块
     * <p> 检查原始流块的内容是否不为空, 以确定当前解析策略是否适用
     *
     * @param context 解析上下文, 不能为 null
     * @param chunk   原始流块, 不能为 null
     * @return 如果原始流块的内容不为空, 则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        String content = chunk.content();
        return content != null;
    }

    /**
     * 解析流数据中的内容并发射为内容块
     * <p> 该方法用于解析传入的流数据块, 提取其中的内容, 并将其作为内容块发射出去.
     * 如果内容为空或为 null, 则直接返回, 不进行任何处理.
     *
     * @param context 解析上下文, 包含解析过程中所需的信息
     * @param chunk   流数据块, 包含需要解析的数据内容
     * @param emitter 用于发射解析后的内容块
     */
    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        if (content == null || content.isEmpty()) {
            return;
        }
        emitter.emit(new StreamChunk(StreamChunkType.CONTENT, content));
    }
}
