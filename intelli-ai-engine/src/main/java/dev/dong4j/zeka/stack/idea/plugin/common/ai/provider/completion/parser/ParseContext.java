package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import lombok.Getter;

/**
 * 解析上下文类
 * <p>用于在解析过程中跟踪和处理特定标签 (如 &lt;think&gt; 和 &lt;/think&gt;) 的状态.
 * 该类维护一个布尔标志表示是否处于思考模式, 并提供缓冲区用于构建和检查标签内容.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public class ParseContext {
    /**
     * 开始思考标签的标记字符串.
     *
     * <p> 该常量用于标识 Markdown 解析中思考片段的开始位置,
     * 对应的结束标签为 {@link #THINK_CLOSE}.</p>
     */
    private static final String THINK_OPEN = "<think>";
    /** 思考标签关闭标记字符串 */
    private static final String THINK_CLOSE = "</think>";

    /** 是否处于思考状态, 用于标记流式解析中是否正在处理标签内容 */
    @Getter
    private boolean inThinking = false;
    /** 标签缓冲区, 用于临时存储待解析的标签字符序列 */
    private final StringBuilder tagBuffer = new StringBuilder();
    /** 兜底提示是否已输出 */
    @Getter
    private boolean fallbackWarningEmitted = false;

    /**
     * 进入思考状态
     * <p> 将当前状态标记为思考中, 用于流式解析过程中维护上下文状态.
     *
     */
    public void enterThinking() {
        inThinking = true;
    }

    /**
     * 退出思考状态
     * <p> 将当前解析上下文的思考状态标记为 false, 表示不再处于思考模式
     *
     * @see #enterThinking() 用于进入思考状态
     */
    public void exitThinking() {
        inThinking = false;
    }

    /**
     * 判断标签缓冲区是否包含内容
     * <p> 检查当前标签缓冲区是否非空, 用于判断是否已收集到完整的标签内容
     *
     * @return 如果标签缓冲区非空则返回 true, 否则返回 false
     */
    public boolean hasTagBuffer() {
        return !tagBuffer.isEmpty();
    }

    /**
     * 将指定字符追加到标签缓冲区
     * <p> 用于累积标签前缀字符, 直到完整标签匹配或清除缓冲区
     *
     * @param ch 要追加的字符
     */
    public void appendTagChar(char ch) {
        tagBuffer.append(ch);
    }

    /**
     * 判断当前标签缓冲区是否为标签前缀
     * <p> 检查标签缓冲区的内容是否以 THINK_OPEN 或 THINK_CLOSE 的开头部分匹配
     *
     * @return 如果标签缓冲区的内容是 THINK_OPEN 或 THINK_CLOSE 的前缀, 则返回 true, 否则返回 false
     */
    public boolean isTagPrefix() {
        String buf = tagBuffer.toString();
        return THINK_OPEN.startsWith(buf) || THINK_CLOSE.startsWith(buf);
    }

    /**
     * 判断标签开启标记是否完整匹配
     * <p> 检查当前标签缓冲区内容是否完全等于开启标签的字符串
     *
     * @return 如果标签缓冲区内容与开启标签完全相等, 则返回 true, 否则返回 false
     */
    public boolean isOpenTagComplete() {
        return THINK_OPEN.contentEquals(tagBuffer);
    }

    /**
     * 判断当前标签缓冲区内容是否与闭合标签匹配
     * <p> 用于识别流式解析中的 &lt;/think&gt; 闭合标签是否完整
     *
     * @return 如果标签缓冲区内容等于 &lt;/think&gt;, 则返回 true
     */
    public boolean isCloseTagComplete() {
        return THINK_CLOSE.contentEquals(tagBuffer);
    }

    /**
     * 消费并清空标签缓冲区中的内容.
     * <p> 该方法会将当前标签缓冲区的内容转换为字符串返回, 并立即清空缓冲区.
     * 通常用于在解析到完整的标签后, 获取并处理缓冲区中的文本.</p>
     *
     * @return 标签缓冲区中当前的文本内容
     */
    public String consumeTagBuffer() {
        String text = tagBuffer.toString();
        tagBuffer.setLength(0);
        return text;
    }

    /**
     * 清除标签缓冲区中的内容
     * <p> 将标签缓冲区重置为空字符串, 用于清除当前累积的标签字符.
     *
     * @since 1.0
     */
    public void clearTagBuffer() {
        tagBuffer.setLength(0);
    }

    /**
     * 标记已输出兜底提示
     * <p> 将兜底提示是否已输出的标志设置为 true, 用于避免重复输出提示信息.
     *
     * @since 1.0
     */
    public void markFallbackWarningEmitted() {
        fallbackWarningEmitted = true;
    }
}
