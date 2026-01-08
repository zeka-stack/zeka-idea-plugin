package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import org.jetbrains.annotations.NotNull;

/**
 * ThinkTagStrategy 类
 * <p> 用于处理包含特殊标签的流数据解析策略, 主要负责识别和处理 &lt; 和 &gt; 标签, 并在检测到特定标签时切换至思考状态.
 * <p> 该策略通过检查内容中是否包含 "<" 或 ">" 来判断是否需要进入思考模式, 并在遇到完整标签时进行文本缓冲区的刷新和状态切换.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class ThinkTagStrategy implements StreamParseStrategy {
    /**
     * 获取当前解析策略的优先级
     *
     * @return 返回该策略的优先级值, 数值越大表示优先级越高
     */
    @Override
    public int priority() {
        return 90;
    }

    /**
     * 判断当前解析上下文是否支持处理指定的原始数据块
     * <p> 该方法用于判断当前解析器是否能够处理给定的原始数据块, 主要依据是上下文状态和内容是否包含标签符号 (< 或 >)
     * <p> 支持条件包括:
     * <ul>
     *   <li> 上下文已缓存标签内容 </li>
     *   <li> 当前处于思考状态 (isInThinking() 为 true)</li>
     *   <li> 数据块内容包含小于号 '<' 或大于号 '>'</li>
     * </ul>
     *
     * @param context 解析上下文, 不能为 null
     * @param chunk   原始数据块, 不能为 null
     * @return 如果满足任一支持条件则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        String content = chunk.content();
        if (content == null || content.isEmpty()) {
            return context.hasTagBuffer();
        }
        return context.hasTagBuffer()
               || context.isInThinking()
               || content.contains("<")
               || content.contains(">");
    }

    /**
     * 解析给定的流块内容
     * <p> 根据上下文中的标签缓冲区和字符内容, 解析文本并触发相应的事件
     * <p> 具体步骤如下:
     * <ol>
     * <li> 检查内容是否为空, 若为空则直接返回 </li>
     * <li> 遍历内容中的每个字符, 根据上下文的状态决定如何处理字符 </li>
     * <li> 如果遇到标签起始字符 '<' 或上下文存在标签缓冲区, 则处理标签相关逻辑 </li>
     * <li> 如果标签完整, 则刷新当前文本缓冲区并清空标签缓冲区 </li>
     * <li> 如果不是标签前缀字符, 则将标签缓冲区的内容追加到文本缓冲区 </li>
     * <li> 最后, 刷新剩余的文本缓冲区内容 </li>
     * </ol>
     *
     * @param context 当前解析上下文
     * @param chunk   需要解析的原始流块
     * @param emitter 用于发出解析结果的流块发射器
     */
    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        if (content == null || content.isEmpty()) {
            return;
        }
        StringBuilder textBuffer = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (context.hasTagBuffer() || ch == '<') {
                context.appendTagChar(ch);
                if (context.isOpenTagComplete()) {
                    flushText(context, textBuffer, emitter);
                    context.clearTagBuffer();
                    context.enterThinking();
                    continue;
                }
                if (context.isCloseTagComplete()) {
                    flushText(context, textBuffer, emitter);
                    context.clearTagBuffer();
                    context.exitThinking();
                    continue;
                }
                if (!context.isTagPrefix()) {
                    textBuffer.append(context.consumeTagBuffer());
                }
                continue;
            }
            textBuffer.append(ch);
        }
        flushText(context, textBuffer, emitter);
    }

    /**
     * 将文本缓冲区中的内容以指定类型发送到输出流
     * <p>根据当前解析上下文的状态, 决定发送的块类型为思考状态 (THINKING) 或普通内容(CONTENT)
     * <p>发送完成后清空文本缓冲区
     *
     * @param context    解析上下文, 包含当前解析状态信息
     * @param textBuffer 文本缓冲区, 存储待发送的字符内容
     * @param emitter    流块发射器, 用于将生成的流块发送出去
     */
    private void flushText(ParseContext context, StringBuilder textBuffer, StreamChunkEmitter emitter) {
        if (textBuffer.isEmpty()) {
            return;
        }
        StreamChunkType type = context.isInThinking() ? StreamChunkType.THINKING : StreamChunkType.CONTENT;
        emitter.emit(new StreamChunk(type, textBuffer.toString()));
        textBuffer.setLength(0);
    }
}
