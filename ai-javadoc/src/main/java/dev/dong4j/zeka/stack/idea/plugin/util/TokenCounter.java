package dev.dong4j.zeka.stack.idea.plugin.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;

/**
 * Token 计数工具类
 * <p>
 * 用于估算文本的 Token 数量，主要用于 AI API 调用的成本估算和上下文限制检查。
 *
 * <p>计算方法说明：
 * <ul>
 *   <li>精确计算：需要使用特定模型的 tokenizer（如 tiktoken），但需要额外依赖</li>
 *   <li>估算计算：基于统计规律进行估算，误差约 ±20%，无需额外依赖</li>
 * </ul>
 *
 * <p>估算规则（基于 OpenAI GPT 系列模型）：
 * <ul>
 *   <li>英文单词：平均 1.3 token/word</li>
 *   <li>英文字符：平均 4 字符 = 1 token</li>
 *   <li>中文字符：平均 1.5-2 字符 = 1 token</li>
 *   <li>数字和符号：约 4 字符 = 1 token</li>
 *   <li>代码：约 3-4 字符 = 1 token（包含缩进和特殊符号）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 快速估算
 * int tokens = TokenCounter.estimateTokens("Hello, World!");
 *
 * // 获取详细统计
 * TokenStats stats = TokenCounter.analyze("你好，世界！Hello, World!");
 * System.out.println("估算 tokens: " + stats.getEstimatedTokens());
 * System.out.println("字符数: " + stats.getTotalChars());
 *
 * // 检查是否超过限制
 * boolean exceedsLimit = TokenCounter.exceedsLimit(text, 4096);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.2.1
 */
public final class TokenCounter {

    /** 英文字符每 token 平均字符数 */
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;

    /** 中文字符每 token 平均字符数 */
    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;

    /** 代码字符每 token 平均字符数（包含特殊符号和缩进） */
    private static final double CODE_CHARS_PER_TOKEN = 3.5;

    /** 中文字符正则表达式 */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5]");

    /** 英文单词正则表达式 */
    private static final Pattern ENGLISH_WORD_PATTERN = Pattern.compile("\\b[a-zA-Z]+\\b");

    /** 代码特征正则表达式（包含常见代码符号） */
    private static final Pattern CODE_PATTERN = Pattern.compile("[{}\\[\\]();,.<>+=\\-*/&|!~^%]");

    /**
     * 私有构造函数，防止实例化
     */
    private TokenCounter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 快速估算文本的 token 数量
     * <p>
     * 使用简化的估算规则，适用于大多数场景。
     * 对于中英文混合文本，会自动识别并采用不同的计算规则。
     *
     * @param text 要计算的文本
     * @return 估算的 token 数量，如果文本为空返回 0
     */
    public static int estimateTokens(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        return analyze(text).getEstimatedTokens();
    }

    /**
     * 估算代码文本的 token 数量
     * <p>
     * 代码通常包含更多的特殊字符和缩进，使用专门的估算规则。
     *
     * @param code 要计算的代码文本
     * @return 估算的 token 数量，如果代码为空返回 0
     */
    public static int estimateCodeTokens(@Nullable String code) {
        if (code == null || code.isEmpty()) {
            return 0;
        }

        // 代码使用更保守的估算：每 3.5 字符约等于 1 token
        int totalChars = code.length();
        return (int) Math.ceil(totalChars / CODE_CHARS_PER_TOKEN);
    }

    /**
     * 详细分析文本并返回统计信息
     * <p>
     * 提供更详细的统计信息，包括字符数、单词数、中英文比例等。
     *
     * @param text 要分析的文本
     * @return 文本统计信息，如果文本为空返回空统计信息
     */
    @NotNull
    public static TokenStats analyze(@Nullable String text) {
        TokenStats stats = new TokenStats();

        if (text == null || text.isEmpty()) {
            return stats;
        }

        // 基础统计
        stats.setTotalChars(text.length());
        stats.setLines(text.split("\n").length);

        // 统计中文字符
        Matcher chineseMatcher = CHINESE_PATTERN.matcher(text);
        int chineseCount = 0;
        while (chineseMatcher.find()) {
            chineseCount++;
        }
        stats.setChineseChars(chineseCount);

        // 统计英文单词
        Matcher englishMatcher = ENGLISH_WORD_PATTERN.matcher(text);
        int englishWords = 0;
        while (englishMatcher.find()) {
            englishWords++;
        }
        stats.setEnglishWords(englishWords);

        // 统计代码特征符号
        Matcher codeMatcher = CODE_PATTERN.matcher(text);
        int codeSymbols = 0;
        while (codeMatcher.find()) {
            codeSymbols++;
        }
        stats.setCodeSymbols(codeSymbols);

        // 计算其他字符数（非中文、非英文字母）
        int otherChars = stats.getTotalChars() - chineseCount -
                         text.replaceAll("[^a-zA-Z]", "").length();
        stats.setOtherChars(Math.max(0, otherChars));

        // 估算 token 数量
        int estimatedTokens = calculateTokens(stats);
        stats.setEstimatedTokens(estimatedTokens);

        return stats;
    }

    /**
     * 根据统计信息计算 token 数量
     *
     * @param stats 文本统计信息
     * @return 估算的 token 数量
     */
    private static int calculateTokens(@NotNull TokenStats stats) {
        // 中文字符的 token 数
        double chineseTokens = stats.getChineseChars() / CHINESE_CHARS_PER_TOKEN;

        // 英文单词的 token 数（平均每个单词 1.3 个 token）
        double englishTokens = stats.getEnglishWords() * 1.3;

        // 其他字符的 token 数
        int englishLetters = stats.getTotalChars() - stats.getChineseChars() - stats.getOtherChars();
        double otherTokens = (englishLetters + stats.getOtherChars()) / ENGLISH_CHARS_PER_TOKEN;

        // 如果有较多代码符号，调整估算
        if (stats.getCodeSymbols() > stats.getTotalChars() * 0.1) {
            // 代码类文本，使用更保守的估算
            return (int) Math.ceil(stats.getTotalChars() / CODE_CHARS_PER_TOKEN);
        }

        // 综合计算，取较大值作为估算结果（偏保守）
        return (int) Math.ceil(Math.max(
            chineseTokens + englishTokens,
            otherTokens
                                       ));
    }

    /**
     * 检查文本的 token 数是否超过指定限制
     *
     * @param text  要检查的文本
     * @param limit token 数量限制
     * @return 如果超过限制返回 true，否则返回 false
     */
    public static boolean exceedsLimit(@Nullable String text, int limit) {
        return estimateTokens(text) > limit;
    }

    /**
     * 计算多个文本的总 token 数
     *
     * @param texts 文本数组
     * @return 总 token 数
     */
    public static int estimateTotalTokens(@NotNull String... texts) {
        int total = 0;
        for (String text : texts) {
            total += estimateTokens(text);
        }
        return total;
    }

    /**
     * 截断文本以符合 token 限制
     * <p>
     * 注意：这是一个粗略的截断方法，实际 token 边界可能不准确。
     * 建议预留 10-20% 的余量。
     *
     * @param text      要截断的文本
     * @param maxTokens 最大 token 数
     * @return 截断后的文本
     */
    @NotNull
    public static String truncateToTokenLimit(@Nullable String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int estimatedTokens = estimateTokens(text);
        if (estimatedTokens <= maxTokens) {
            return text;
        }

        // 计算需要保留的字符比例（预留 10% 余量）
        double ratio = (maxTokens * 0.9) / estimatedTokens;
        int targetLength = (int) (text.length() * ratio);

        if (targetLength <= 0) {
            return "";
        }

        if (targetLength >= text.length()) {
            return text;
        }

        // 截断到最近的完整单词或句子
        String truncated = text.substring(0, targetLength);

        // 尝试在最后一个空格或句号处截断
        int lastSpace = truncated.lastIndexOf(' ');
        int lastPeriod = truncated.lastIndexOf('。');
        int lastDot = truncated.lastIndexOf('.');

        int cutPoint = Math.max(Math.max(lastSpace, lastPeriod), lastDot);
        if (cutPoint > targetLength * 0.8) {
            truncated = truncated.substring(0, cutPoint);
        }

        return truncated + "...";
    }

    /**
     * Token 统计信息
     */
    @Data
    public static class TokenStats {
        /** 估算的 token 数量 */
        private int estimatedTokens;

        /** 总字符数 */
        private int totalChars;

        /** 中文字符数 */
        private int chineseChars;

        /** 英文单词数 */
        private int englishWords;

        /** 其他字符数 */
        private int otherChars;

        /** 代码符号数 */
        private int codeSymbols;

        /** 行数 */
        private int lines;

        /**
         * 获取平均每行 token 数
         *
         * @return 平均每行 token 数
         */
        public double getAvgTokensPerLine() {
            return lines > 0 ? (double) estimatedTokens / lines : 0;
        }

        /**
         * 获取中文字符占比
         *
         * @return 中文字符占比（0-1）
         */
        public double getChineseRatio() {
            return totalChars > 0 ? (double) chineseChars / totalChars : 0;
        }

        /**
         * 判断是否主要是代码
         *
         * @return 如果代码符号占比超过 10% 返回 true
         */
        public boolean isProbablyCode() {
            return totalChars > 0 && (double) codeSymbols / totalChars > 0.1;
        }

        @Override
        public String toString() {
            return String.format(
                "TokenStats{tokens=%d, chars=%d, chinese=%d(%.1f%%), words=%d, lines=%d, avgTokens/line=%.1f, isCode=%b}",
                estimatedTokens, totalChars, chineseChars, getChineseRatio() * 100,
                englishWords, lines, getAvgTokensPerLine(), isProbablyCode()
                                );
        }
    }
}

