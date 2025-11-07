package dev.dong4j.zeka.stack.idea.plugin.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * JavaDoc 文本格式化工具类
 *
 * <p>用于格式化 AI 生成的 JavaDoc 注释，提升可读性和规范性。
 *
 * <p>主要功能：
 * <ul>
 *   <li>在中英文之间添加空格，提升可读性</li>
 *   <li>将中文标点符号替换为英文标点符号</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * String formatted = JavaDocFormatter.format("这是一个User类，用于处理用户数据。");
 * // 结果: "这是一个 User 类, 用于处理用户数据."
 * </pre>
 *
 * @author dong4j
 * @version 1.4.0
 * @since 1.4.0
 */
public final class JavaDocFormatter {

    /**
     * 中文字符正则表达式（包括中文标点）
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5\\u3000-\\u303F\\uFF00-\\uFFEF]");

    /**
     * 英文字母和数字正则表达式
     */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z0-9]");

    /**
     * 中文标点符号映射表
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
     * 私有构造函数，防止实例化
     */
    private JavaDocFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 格式化 JavaDoc 文本
     *
     * <p>对 AI 生成的 JavaDoc 文本进行格式化处理：
     * <ol>
     *   <li>在中英文之间添加空格</li>
     *   <li>将中文标点符号替换为英文标点符号</li>
     * </ol>
     *
     * <p>处理示例：
     * <ul>
     *   <li>输入: "这是一个User类，用于处理用户数据。"</li>
     *   <li>输出: "这是一个 User 类, 用于处理用户数据."</li>
     * </ul>
     *
     * @param text 原始 JavaDoc 文本
     * @return 格式化后的文本
     */
    @NotNull
    public static String format(@NotNull String text) {
        if (text.isEmpty()) {
            return text;
        }

        // 1. 替换中文标点符号为英文标点符号
        String result = replaceChinesePunctuation(text);

        // 2. 在中英文之间添加空格
        result = addSpaceBetweenChineseAndEnglish(result);

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

    /**
     * 在中英文之间添加空格
     *
     * <p>处理规则：
     * <ul>
     *   <li>中文 + 英文/数字 -> 中文 + 空格 + 英文/数字</li>
     *   <li>英文/数字 + 中文 -> 英文/数字 + 空格 + 中文</li>
     *   <li>避免在 @ 符号和标签名之间添加空格（如 @param、@return）</li>
     *   <li>避免重复添加空格</li>
     * </ul>
     *
     * @param text 原始文本
     * @return 添加空格后的文本
     */
    @NotNull
    private static String addSpaceBetweenChineseAndEnglish(@NotNull String text) {
        if (text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            result.append(current);

            // 如果当前字符是 @ 符号，跳过后续的标签名，避免在 @param、@return 等标签中添加空格
            if (current == '@') {
                // 跳过 @ 后面的标签名（通常是字母），但不添加空格
                int j = i + 1;
                while (j < chars.length && Character.isLetter(chars[j])) {
                    result.append(chars[j]);
                    j++;
                }
                // 更新索引，跳过已处理的字符
                i = j - 1;
                continue;
            }

            // 如果当前字符是中文，检查下一个字符
            if (i < chars.length - 1 && isChinese(current)) {
                char next = chars[i + 1];
                // 如果下一个字符是英文或数字，添加空格
                if (isEnglishOrDigit(next)) {
                    // 检查是否已经有空格
                    if (result.length() == 0 || result.charAt(result.length() - 1) != ' ') {
                        result.append(' ');
                    }
                }
            }
            // 如果当前字符是英文或数字，检查下一个字符
            else if (i < chars.length - 1 && isEnglishOrDigit(current)) {
                char next = chars[i + 1];
                // 如果下一个字符是中文，添加空格
                if (isChinese(next)) {
                    // 检查是否已经有空格
                    if (result.length() == 0 || result.charAt(result.length() - 1) != ' ') {
                        result.append(' ');
                    }
                }
            }
        }

        // 清理多余的空格（连续的空格合并为一个）
        return result.toString().replaceAll(" {2,}", " ");
    }

    /**
     * 判断字符是否为中文字符
     *
     * @param c 字符
     * @return 如果是中文字符返回 true
     */
    private static boolean isChinese(char c) {
        return CHINESE_PATTERN.matcher(String.valueOf(c)).matches();
    }

    /**
     * 判断字符是否为英文字母或数字
     *
     * @param c 字符
     * @return 如果是英文字母或数字返回 true
     */
    private static boolean isEnglishOrDigit(char c) {
        return ENGLISH_PATTERN.matcher(String.valueOf(c)).matches();
    }
}

