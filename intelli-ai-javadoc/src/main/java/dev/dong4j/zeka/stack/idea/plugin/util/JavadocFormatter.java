package dev.dong4j.zeka.stack.idea.plugin.util;

import org.jetbrains.annotations.NotNull;

/**
 * Javadoc 格式化工具类
 * <p>
 * 提供 Javadoc 注释的格式化功能, 包括中文标点符号替换和中英文间空格添加,
 * 用于规范化 Javadoc 注释的格式, 提升代码文档的可读性和一致性
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class JavadocFormatter {
    /**
     * 中英文标点符号映射表
     * <p>
     * 用于将中文标点转换为对应的英文标点符号
     */
    private static final String[][] PUNCTUATION_MAP = {
        {"，", ","},   // 中文逗号 -> 英文逗号
        {"。", "."},   // 中文句号 -> 英文句号
        {"；", ";"},   // 中文分号 -> 英文分号
        {"：", ":"},   // 中文冒号 -> 英文冒号
        {"？", "?"},   // 中文问号 -> 英文问号
        {"！", "!"},   // 中文感叹号 -> 英文感叹号
        {"（", "("},   // 中文左括号 -> 英文左括号
        {"）", ")"},   // 中文右括号 -> 英文右括号
        {"【", "["},   // 中文左方括号 -> 英文左方括号
        {"】", "]"},   // 中文右方括号 -> 英文右方括号
        {"《", "<"},   // 中文左书名号 -> 小于号
        {"》", ">"},   // 中文右书名号 -> 大于号
        {"、", ","},   // 中文顿号 -> 英文逗号
        {"…", "..."}, // 中文省略号 -> 三个点
    };

    /**
     * Pangu 实例，用于处理文本相关的操作
     * <p>
     * 该实例在类加载时初始化，提供全局可用的文本处理功能
     */
    private static final Pangu pangu = new Pangu();

    /**
     * 私有构造函数，防止实例化
     */
    private JavadocFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 格式化 Javadoc 文本
     *
     * <p>对 AI 生成的 Javadoc 文本进行格式化处理：
     * <ol>
     *   <li>在中英文之间添加空格（可选）</li>
     *   <li>将中文标点符号替换为英文标点符号（可选）</li>
     * </ol>
     *
     * <p>处理示例：
     * <ul>
     *   <li>输入: "这是一个User类，用于处理用户数据。"</li>
     *   <li>输出: "这是一个 User 类, 用于处理用户数据."</li>
     * </ul>
     *
     * @param text 原始 Javadoc 文本
     * @return 格式化后的文本
     */
    @NotNull
    public static String format(@NotNull String text) {
        return format(text, true, true);
    }

    /**
     * 格式化 Javadoc 文本（支持配置）
     *
     * <p>对 AI 生成的 Javadoc 文本进行格式化处理，根据配置决定是否执行各项格式化操作。
     *
     * @param text                             原始 Javadoc 文本
     * @param addSpaceBetweenChineseAndEnglish 是否在中英文之间添加空格
     * @param replaceChinesePunctuation        是否将中文标点符号替换为英文标点符号
     * @return 格式化后的文本
     */
    @NotNull
    public static String format(@NotNull String text,
                                boolean addSpaceBetweenChineseAndEnglish,
                                boolean replaceChinesePunctuation) {
        if (text.isEmpty()) {
            return text;
        }

        String result = text;

        // 1. 替换中文标点符号为英文标点符号
        if (replaceChinesePunctuation) {
            result = replaceChinesePunctuation(result);
        }

        // 2. 在中英文之间添加空格
        if (addSpaceBetweenChineseAndEnglish) {
            result = pangu.spacingText(result);
        }

        return result;
    }

    /**
     * 替换中文标点符号为英文标点符号
     *
     * @param text 原始文本
     * @return 替换后的文本
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

