package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 降级文本解析策略类
 * <p> 实现 {@link StreamParseStrategy} 接口, 用于在其他解析策略失败时作为兜底方案, 直接将原始流内容作为内容块输出.
 * <p> 该策略优先级为 0, 表示最低优先级, 仅在其他策略不支持时被调用.
 * <p> 使用场景: 当流数据无法被特定解析器处理时, 通过此策略将原始内容保留并传递给下游处理器.
 * <p> 示例用法:
 * <pre>{@code
 * FallbackTextStrategy strategy = new FallbackTextStrategy();
 * // 该策略将始终支持所有内容, 仅在内容非空时输出
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class FallbackTextStrategy implements StreamParseStrategy {
    /** 降级解析时显示的警告信息, 用于提示用户查看相关讨论链接 */
    private static final String FALLBACK_WARNING =
        AICommonBundle.message("stream.parse.fallback.warning", "https://github.com/zeka-stack/zeka-idea-plugin/discussions");

    /**
     * 返回优先级值
     * <p> 该方法返回当前策略的优先级, 值越小优先级越高. 在此实现中, 优先级被固定为 0.
     *
     * @return 优先级值, 固定为 0
     */
    @Override
    public int priority() {
        return 0;
    }

    /**
     * 判断是否支持给定的解析上下文和原始流块
     * <p> 作为兜底文本策略, 该方法始终返回 true, 表示支持所有解析上下文和原始流块
     *
     * @param context 解析上下文, 不能为 null
     * @param chunk   原始流块, 不能为 null
     * @return 始终返回 true, 表示支持该解析上下文和原始流块
     */
    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        return true;
    }

    /**
     * 解析原始流数据并发出内容类型的流块.
     * <p> 该方法为兜底策略, 当其他解析策略不适用时使用. 如果数据内容非空, 则将其作为 CONTENT 类型的流块发出,
     * 并标记兜底策略已实际输出内容 (fallbackUsed). 当流结束时, 仅在兜底策略曾输出过内容且未输出过提示时,
     * 追加一次兜底警告 (fallbackWarningEmitted) 以提醒用户当前使用的是兜底解析.
     *
     * @param context 解析上下文, 提供当前解析所需的环境信息
     * @param chunk   待解析的原始流数据块
     * @param emitter 用于发出解析后的流块对象
     */
    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        if (content != null && !content.isEmpty()) {
            context.markFallbackUsed();
            emitter.emit(new StreamChunk(StreamChunkType.CONTENT, content));
        }
        // 如果是最后一行数据, 且使用过此策略输出过内容的, 如果没有警告过那就输出一行警告, 因为只要走了这个策略那就意味着有部分特殊的响应体没有覆盖到, 所以需要反馈
        if (chunk.isDone() && context.isFallbackUsed() && !context.isFallbackWarningEmitted()) {
            context.markFallbackWarningEmitted();
            emitter.emit(new StreamChunk(StreamChunkType.NOTICE, FALLBACK_WARNING));
        }
    }
}
