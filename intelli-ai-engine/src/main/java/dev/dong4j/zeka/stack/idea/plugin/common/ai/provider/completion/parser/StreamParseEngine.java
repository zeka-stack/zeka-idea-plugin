package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 流解析引擎类
 * <p> 负责根据给定的解析上下文和原始流块, 应用一系列解析策略来解析流数据.
 * <p> 通过优先级排序解析策略, 并在解析过程中选择第一个支持的策略进行处理.
 * <p> 提供了静态方法 `createDefault` 来创建默认的解析引擎实例, 该实例包含了预定义的一组解析策略.
 *
 * @param <pre>{@code StreamParseEngine engine = StreamParseEngine.createDefault();
 *                    engine.parse(context, rawStreamChunk, streamChunkEmitter);
 *                    }</pre>
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public final class StreamParseEngine {
    /**
     * 解析策略列表
     * <p> 按优先级降序排列, 用于流式解析过程中的策略匹配与执行
     */
    private final List<StreamParseStrategy> strategies;

    /**
     * 初始化流式解析引擎
     * <p> 根据传入的解析策略列表创建引擎实例, 并按优先级降序排序策略列表
     *
     * @param strategies 解析策略列表, 不能为 null, 引擎将按优先级顺序依次尝试匹配和执行策略
     */
    public StreamParseEngine(List<StreamParseStrategy> strategies) {
        this.strategies = new ArrayList<>(strategies);
        this.strategies.sort(Comparator.comparingInt(StreamParseStrategy::priority).reversed());
    }

    /**
     * 解析流式数据块
     * <p> 遍历注册的解析策略, 找到第一个支持当前上下文和数据块的策略, 并调用其解析方法, 解析完成后立即终止遍历.
     * <p> 每个策略仅消费当前数据块, 确保责任链中只有一个策略处理当前 chunk.
     *
     * @param context 解析上下文, 包含当前解析状态和配置
     * @param chunk   待解析的原始流数据块
     * @param emitter 数据块解析后的输出发射器, 用于传递解析结果
     */
    public void parse(ParseContext context, RawStreamChunk chunk, StreamChunkEmitter emitter) {
        for (StreamParseStrategy strategy : strategies) {
            if (strategy.supports(context, chunk)) {
                strategy.parse(context, chunk, emitter);
                break;
            }
        }
    }

    /**
     * 创建默认的流式解析引擎实例
     * <p> 返回一个包含默认策略的 StreamParseEngine 实例, 策略按照优先级降序排列
     *
     * @return 默认的流式解析引擎实例
     */
    public static StreamParseEngine createDefault() {
        List<StreamParseStrategy> strategies = List.of(
            new ReasoningFieldStrategy(),
            new ThinkTagStrategy(),
            new MessageContentStrategy(),
            new FallbackTextStrategy()
                                                      );
        return new StreamParseEngine(strategies);
    }
}
