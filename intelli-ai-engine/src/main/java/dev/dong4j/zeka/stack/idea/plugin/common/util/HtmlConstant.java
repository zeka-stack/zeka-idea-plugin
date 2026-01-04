package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.openapi.util.text.HtmlChunk;

/**
 * HTML 常量接口
 * <p> 提供一些常用的 HTML 字符串包装方法, 用于生成常见的 HTML 结构.
 * <p> 该接口主要用于简化 HTML 字符串的构建, 支持将普通文本转换为带 HTML 标签的字符串.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public interface HtmlConstant {

    /**
     * 包装 HTML 文本
     * <p> 将给定的文本内容包装成完整的 HTML 格式
     *
     * @param text 文本内容
     * @return 包装后的 HTML 格式的文本
     */
    static String wrapHtml(String text) {
        return HtmlChunk.raw(text)
            .wrapWith(HtmlChunk.html())
            .toString();
    }

    /**
     * 包装 HTML body 文本
     * <p> 将给定的文本内容包装成 HTML body 格式的字符串
     *
     * @param text 文本内容
     * @return 包装后的 HTML body 格式的文本
     */
    static String wrapBody(String text) {
        return HtmlChunk.raw(text)
            .wrapWith(HtmlChunk.body())
            .wrapWith(HtmlChunk.html())
            .toString();
    }

    /**
     * 包装 HTML 粗体文本
     * <p> 将给定的文本内容转换为 HTML 粗体格式的字符串
     *
     * @param text 文本内容
     * @return HTML 粗体格式的文本
     */
    static String wrapBoldHtml(String text) {
        return HtmlChunk.raw(text)
            .bold()
            .wrapWith(HtmlChunk.body())
            .wrapWith(HtmlChunk.html())
            .toString();
    }
}

