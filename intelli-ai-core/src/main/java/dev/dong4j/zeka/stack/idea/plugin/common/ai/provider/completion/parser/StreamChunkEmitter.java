package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

/**
 * 流数据块发射器接口
 * <p> 提供流式数据块发射的通用接口规范, 用于将 {@link io.dong4j.azkaban.entity.StreamChunk} 对象
 * 发送到下游处理系统. 该接口定义了数据块发射的核心方法, 所有实现类需提供具体的发射逻辑
 * <p> 此接口适用于以下场景:
 * <ul>
 *   <li> 流式数据传输和处理 </li>
 *   <li> 事件流推送系统 </li>
 *   <li> 批处理与流处理的数据分发 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public interface StreamChunkEmitter {
    /**
     * 发送流式块数据到输出通道
     * <p> 将指定的流式块数据发送到输出通道进行处理或传输
     *
     * @param chunk 流式块数据对象, 不能为 null
     */
    void emit(StreamChunk chunk);
}
