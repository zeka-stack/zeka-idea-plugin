package dev.dong4j.zeka.stack.idea.plugin.util;

import dev.dong4j.zeka.stack.idea.plugin.util.TokenCounter.TokenStats;

/**
 * TokenCounter 演示程序
 *
 * <p>展示 TokenCounter 工具类的各种使用方式
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.2.1
 */
public class TokenCounterDemo {

    public static void main(String[] args) {
        System.out.println("========== TokenCounter 工具类演示 ==========\n");

        // 示例 1: 英文文本
        demo1_EnglishText();

        // 示例 2: 中文文本
        demo2_ChineseText();

        // 示例 3: 中英文混合
        demo3_MixedText();

        // 示例 4: 代码文本
        demo4_CodeText();

        // 示例 5: JavaDoc 注释
        demo5_JavaDocComment();

        // 示例 6: 检查限制和截断
        demo6_LimitAndTruncate();

        // 示例 7: 多文本计算
        demo7_MultipleTexts();
    }

    private static void demo1_EnglishText() {
        System.out.println("【示例 1】英文文本");
        System.out.println("--------------------------------------------------");

        String text = "Hello, World! This is a test of the token counter utility. " +
                      "It can estimate the number of tokens in a given text.";

        int tokens = TokenCounter.estimateTokens(text);
        TokenStats stats = TokenCounter.analyze(text);

        System.out.println("文本: " + text);
        System.out.println("\n快速估算: " + tokens + " tokens");
        System.out.println("详细统计: " + stats);
        System.out.println();
    }

    private static void demo2_ChineseText() {
        System.out.println("【示例 2】中文文本");
        System.out.println("--------------------------------------------------");

        String text = "这是一个用于计算 Token 数量的工具类。" +
                      "它可以估算给定文本中的 Token 数量，支持中英文混合文本。";

        int tokens = TokenCounter.estimateTokens(text);
        TokenStats stats = TokenCounter.analyze(text);

        System.out.println("文本: " + text);
        System.out.println("\n快速估算: " + tokens + " tokens");
        System.out.println("详细统计: " + stats);
        System.out.println();
    }

    private static void demo3_MixedText() {
        System.out.println("【示例 3】中英文混合");
        System.out.println("--------------------------------------------------");

        String text = "IntelliDoc Assistant 是一个使用 AI 生成 JavaDoc 的插件。\n" +
                      "It supports multiple AI providers including QianWen and Ollama.\n" +
                      "支持多种 AI 服务提供商，包括通义千问和本地 Ollama 模型。";

        TokenStats stats = TokenCounter.analyze(text);

        System.out.println("文本:\n" + text);
        System.out.println("\n估算 tokens: " + stats.getEstimatedTokens());
        System.out.println("总字符数: " + stats.getTotalChars());
        System.out.println("中文字符: " + stats.getChineseChars() +
                           " (" + String.format("%.1f%%", stats.getChineseRatio() * 100) + ")");
        System.out.println("英文单词: " + stats.getEnglishWords());
        System.out.println("行数: " + stats.getLines());
        System.out.println("平均每行: " + String.format("%.1f tokens", stats.getAvgTokensPerLine()));
        System.out.println();
    }

    private static void demo4_CodeText() {
        System.out.println("【示例 4】代码文本");
        System.out.println("--------------------------------------------------");

        String code = "public class TokenCounter {\n" +
                      "    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;\n" +
                      "    \n" +
                      "    public static int estimateTokens(String text) {\n" +
                      "        if (text == null || text.isEmpty()) {\n" +
                      "            return 0;\n" +
                      "        }\n" +
                      "        return analyze(text).getEstimatedTokens();\n" +
                      "    }\n" +
                      "}";

        int normalEstimate = TokenCounter.estimateTokens(code);
        int codeEstimate = TokenCounter.estimateCodeTokens(code);
        TokenStats stats = TokenCounter.analyze(code);

        System.out.println("代码:\n" + code);
        System.out.println("\n普通估算: " + normalEstimate + " tokens");
        System.out.println("代码估算: " + codeEstimate + " tokens");
        System.out.println("是否识别为代码: " + stats.isProbablyCode());
        System.out.println("代码符号数: " + stats.getCodeSymbols());
        System.out.println();
    }

    private static void demo5_JavaDocComment() {
        System.out.println("【示例 5】JavaDoc 注释");
        System.out.println("--------------------------------------------------");

        String javaDoc = "/**\n" +
                         " * Token 计数工具类\n" +
                         " * <p>\n" +
                         " * 用于估算文本的 Token 数量\n" +
                         " * This utility class estimates the number of tokens in a text.\n" +
                         " *\n" +
                         " * @param text 要计算的文本\n" +
                         " * @return 估算的 token 数量\n" +
                         " * @author dong4j\n" +
                         " */";

        TokenStats stats = TokenCounter.analyze(javaDoc);

        System.out.println("JavaDoc:\n" + javaDoc);
        System.out.println("\n" + stats);
        System.out.println();
    }

    private static void demo6_LimitAndTruncate() {
        System.out.println("【示例 6】检查限制和截断");
        System.out.println("--------------------------------------------------");

        String longText = "这是一个很长的文本示例。" +
                          "It contains multiple sentences in both Chinese and English. " +
                          "我们将测试如何检查文本是否超过限制，以及如何截断文本。" +
                          "The truncation should happen at a reasonable boundary like a period or space. " +
                          "这样可以保持文本的完整性和可读性。";

        int tokens = TokenCounter.estimateTokens(longText);
        int limit = 30;

        System.out.println("原文本: " + longText);
        System.out.println("\n估算 tokens: " + tokens);
        System.out.println("限制: " + limit + " tokens");
        System.out.println("是否超过限制: " + TokenCounter.exceedsLimit(longText, limit));

        if (TokenCounter.exceedsLimit(longText, limit)) {
            String truncated = TokenCounter.truncateToTokenLimit(longText, limit);
            int truncatedTokens = TokenCounter.estimateTokens(truncated);

            System.out.println("\n截断后文本: " + truncated);
            System.out.println("截断后 tokens: " + truncatedTokens);
        }
        System.out.println();
    }

    private static void demo7_MultipleTexts() {
        System.out.println("【示例 7】多文本计算");
        System.out.println("--------------------------------------------------");

        String systemPrompt = "你是一个专业的 Java 文档生成助手。";
        String userPrompt = "请为以下方法生成 JavaDoc 注释：";
        String code = "public void processTask(Task task) { }";

        int tokens1 = TokenCounter.estimateTokens(systemPrompt);
        int tokens2 = TokenCounter.estimateTokens(userPrompt);
        int tokens3 = TokenCounter.estimateCodeTokens(code);
        int total = TokenCounter.estimateTotalTokens(systemPrompt, userPrompt, code);

        System.out.println("系统提示: \"" + systemPrompt + "\"");
        System.out.println("  → " + tokens1 + " tokens");

        System.out.println("\n用户提示: \"" + userPrompt + "\"");
        System.out.println("  → " + tokens2 + " tokens");

        System.out.println("\n代码: \"" + code + "\"");
        System.out.println("  → " + tokens3 + " tokens");

        System.out.println("\n总计: " + total + " tokens");
        System.out.println("验证: " + tokens1 + " + " + tokens2 + " + " + tokens3 +
                           " = " + (tokens1 + tokens2 + tokens3));

        // 估算成本（假设 $0.0005 / 1K tokens）
        double cost = (total / 1000.0) * 0.0005;
        System.out.println("预估成本: $" + String.format("%.6f", cost));
        System.out.println();
    }
}

