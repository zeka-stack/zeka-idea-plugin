package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;

/**
 * 流解析策略接口
 * <p> 定义了一组方法, 用于指定如何解析流数据. 每个实现该接口的策略都需要提供优先级, 是否支持特定上下文和数据块的解析能力, 以及具体的解析逻辑.
 * <p> 实现该接口时, 需要确保正确地设置优先级, 并在支持的情况下调用解析方法.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public interface StreamParseStrategy {
    /**
     * 获取解析策略的优先级
     * <p> 返回该解析策略的优先级数值, 数值越小优先级越高
     *
     * @return 优先级数值
     */
    int priority();

    /**
     * 判断是否支持指定的解析上下文和原始流数据块
     * <p> 根据解析上下文和流数据块的特征判断当前解析策略是否适用
     *
     * @param context 解析上下文, 不能为 null
     * @param chunk   原始流数据块, 不能为 null
     * @return 如果支持解析则返回 true, 否则返回 false
     */
    boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk);

    /**
     * 解析流式数据块
     * <p> 根据指定的解析策略, 对流式数据块进行解析, 并通过指定的发射器将解析结果发送出去
     *
     * @param context 解析上下文, 包含解析过程中所需的环境信息
     * @param chunk   流式数据块, 表示当前需要解析的数据内容
     * @param emitter 数据发射器, 用于将解析后的数据发送到下游处理组件
     */
    void parse(@NotNull ParseContext context, @NotNull RawStreamChunk chunk, @NotNull StreamChunkEmitter emitter);
}
