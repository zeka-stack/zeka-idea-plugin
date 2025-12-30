package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI Stream Response Listener
 *
 * @author dong4j
 * @date 2025-12-30 18:45:32
 * @version hello.world
 * @since hello.world
 */
public interface AIStreamResponseListener {

    /**
     * 流式响应开始
     */
    default void onStart() {}

    /**
     * 接收增量内容块
     *
     * @param chunk 增量内容块
     */
    default void onChunk(@NotNull String chunk) {}

    /**
     * 流式响应完成
     * <p> 当流式响应完成后调用此方法, 提供完整的文本内容
     *
     * @param fullText 完整的文本内容
     */
    default void onComplete(@NotNull String fullText) {}

    /**
     * 流式响应错误
     *
     * @param error 错误信息
     * @param exception 异常对象
     */
    default void onError(@NotNull String error, @Nullable Throwable exception) {}
}
