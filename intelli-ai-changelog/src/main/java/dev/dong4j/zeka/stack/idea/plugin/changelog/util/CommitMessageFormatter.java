package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.Pangu;

/**
 * 提交消息格式化工具类
 * <p> 该类用于格式化提交消息, 主要包括去除代码围栏, 替换中文标点符号为英文标点符号等功能.
 * 通过调用静态方法 `format` 可以对给定的文本进行格式化处理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public final class CommitMessageFormatter {
    /**
     * 中文标点符号与英文标点符号的映射表
     * <p> 用于将中文标点符号转换为对应的英文标点符号
     *
     * @see #replaceChinesePunctuation(String)
     */
    private static final String[][] PUNCTUATION_MAP = {
        {"，", ","},
        {"。", "."},
        {"；", ";"},
        {"：", ":"},
        {"？", "?"},
        {"！", "!"},
        {"（", "("},
        {"）", ")"},
        {"【", "["},
        {"】", "]"},
        {"《", "<"},
        {"》", ">"},
        {"、", ","},
        {"…", "..."},
        };

    /**
     * Pangu 实例, 用于中文文本的全角半角转换和空格处理
     *
     * @see Pangu
     */
    private static final Pangu pangu = new Pangu();

    /**
     * 私有构造函数
     * <p> 此构造函数被标记为私有且抛出异常, 以防止实例化该工具类
     *
     */
    private CommitMessageFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 格式化提交记录文本
     * <p> 该方法会对输入的文本进行一系列处理, 包括调整中文字符间距, 去除代码围栏符号以及替换中文标点符号为英文标点符号.
     *
     * @param text 模型输出内容
     * @return 格式化后的提交记录
     */
    @NotNull
    public static String format(@NotNull String text) {
        if (text.isEmpty()) {
            return text;
        }

        String result = text;

        // 1. 在中英文之间添加空格
        result = pangu.spacingText(result);

        // 2. 删除前后的代码包裹符号
        result = stripCodeFences(result);

        // 3. 将中文标点替换为英文标点
        result = replaceChinesePunctuation(result);

        return result.trim();
    }

    /**
     * 去除代码围栏标记
     * <p> 去除字符串开头和结尾的 "```" 标记, 并在处理后进行修剪
     *
     * @param text 输入的字符串
     * @return 处理后的字符串, 去除了开头和结尾的 "```" 标记
     */
    @NotNull
    private static String stripCodeFences(@NotNull String text) {
        String result = text.trim();
        if (result.startsWith("```")) {
            int firstNewline = result.indexOf('\n');
            result = firstNewline >= 0 ? result.substring(firstNewline + 1) : "";
        }
        if (result.endsWith("```")) {
            int lastFence = result.lastIndexOf("```");
            result = lastFence > 0 ? result.substring(0, lastFence) : "";
        }
        return result.trim();
    }

    /**
     * 替换中文标点符号为对应的英文标点符号
     * <p> 遍历预定义的标点符号映射数组, 并将文本中的中文标点符号替换为对应的英文标点符号
     *
     * @param text 输入文本
     * @return 处理后的文本, 其中中文标点符号已被替换为英文标点符号
     */
    @NotNull
    private static String replaceChinesePunctuation(@NotNull String text) {
        String result = text;
        for (String[] mapping : PUNCTUATION_MAP) {
            result = result.replace(mapping[0], mapping[1]);
        }
        return result;
    }
}
