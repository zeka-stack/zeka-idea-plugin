package dev.dong4j.zeka.stack.idea.javadoc.util;

import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.javadoc.util.TokenCounter.TokenStats;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenCounter 测试类
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.2.1
 */
class TokenCounterTest {

    @Test
    void testEstimateTokens_EmptyText() {
        assertThat(TokenCounter.estimateTokens(null)).isZero();
        assertThat(TokenCounter.estimateTokens("")).isZero();
    }

    @Test
    void testEstimateTokens_EnglishText() {
        String text = "Hello, World! This is a test.";
        int tokens = TokenCounter.estimateTokens(text);

        // 英文文本：约 30 个字符，预期约 7-8 个 token
        assertThat(tokens).isBetween(6, 10);
    }

    @Test
    void testEstimateTokens_ChineseText() {
        String text = "你好，世界！这是一个测试。";
        int tokens = TokenCounter.estimateTokens(text);

        // 中文文本：14 个字符，预期约 9-10 个 token
        assertThat(tokens).isBetween(8, 12);
    }

    @Test
    void testEstimateTokens_MixedText() {
        String text = "你好 Hello 世界 World！这是 a test 测试。";
        int tokens = TokenCounter.estimateTokens(text);

        // 中英文混合，预期约 15-20 个 token
        assertThat(tokens).isBetween(12, 22);
    }

    @Test
    void testEstimateCodeTokens() {
        String code = "public class Test {\n" +
                      "    private String name;\n" +
                      "    public void test() {\n" +
                      "        System.out.println(\"test\");\n" +
                      "    }\n" +
                      "}";
        int tokens = TokenCounter.estimateCodeTokens(code);

        // 代码约 130 个字符，预期约 35-40 个 token
        assertThat(tokens).isBetween(30, 45);
    }

    @Test
    void testAnalyze_EmptyText() {
        TokenStats stats = TokenCounter.analyze(null);
        assertThat(stats.getTotalChars()).isZero();
        assertThat(stats.getEstimatedTokens()).isZero();

        stats = TokenCounter.analyze("");
        assertThat(stats.getTotalChars()).isZero();
        assertThat(stats.getEstimatedTokens()).isZero();
    }

    @Test
    void testAnalyze_ChineseText() {
        String text = "你好，世界！\n这是一个测试。";
        TokenStats stats = TokenCounter.analyze(text);

        assertThat(stats.getTotalChars()).isEqualTo(16);
        assertThat(stats.getChineseChars()).isEqualTo(10);
        assertThat(stats.getLines()).isEqualTo(2);
        assertThat(stats.getEstimatedTokens()).isGreaterThan(0);
        assertThat(stats.getChineseRatio()).isGreaterThan(0.5);
    }

    @Test
    void testAnalyze_EnglishText() {
        String text = "Hello World\nThis is a test";
        TokenStats stats = TokenCounter.analyze(text);

        assertThat(stats.getTotalChars()).isEqualTo(26);
        assertThat(stats.getChineseChars()).isZero();
        assertThat(stats.getEnglishWords()).isEqualTo(6);
        assertThat(stats.getLines()).isEqualTo(2);
        assertThat(stats.getEstimatedTokens()).isGreaterThan(0);
        assertThat(stats.isProbablyCode()).isFalse();
    }

    @Test
    void testAnalyze_CodeText() {
        String code = "public void test() { System.out.println(\"test\"); }";
        TokenStats stats = TokenCounter.analyze(code);

        assertThat(stats.getCodeSymbols()).isGreaterThan(5);
        assertThat(stats.isProbablyCode()).isTrue();
    }

    @Test
    void testAnalyze_MixedText() {
        String text = "这是一个 Java 测试：public class Test {}";
        TokenStats stats = TokenCounter.analyze(text);

        assertThat(stats.getChineseChars()).isGreaterThan(0);
        assertThat(stats.getEnglishWords()).isGreaterThan(0);
        assertThat(stats.getCodeSymbols()).isGreaterThan(0);
    }

    @Test
    void testExceedsLimit() {
        String shortText = "Hello";
        String longText = "Hello ".repeat(1000); // 约 1500 tokens

        assertThat(TokenCounter.exceedsLimit(shortText, 100)).isFalse();
        assertThat(TokenCounter.exceedsLimit(longText, 100)).isTrue();
    }

    @Test
    void testEstimateTotalTokens() {
        String text1 = "Hello World";
        String text2 = "你好世界";
        String text3 = "Test 测试";

        int total = TokenCounter.estimateTotalTokens(text1, text2, text3);
        int sum = TokenCounter.estimateTokens(text1) +
                  TokenCounter.estimateTokens(text2) +
                  TokenCounter.estimateTokens(text3);

        assertThat(total).isEqualTo(sum);
    }

    @Test
    void testTruncateToTokenLimit_NoTruncation() {
        String text = "Hello World";
        String result = TokenCounter.truncateToTokenLimit(text, 100);

        assertThat(result).isEqualTo(text);
    }

    @Test
    void testTruncateToTokenLimit_WithTruncation() {
        String text = "Hello World. This is a very long text that needs to be truncated. " +
                      "It contains multiple sentences and should be cut at a reasonable point.";
        String result = TokenCounter.truncateToTokenLimit(text, 10);

        assertThat(result).isNotEqualTo(text);
        assertThat(result).endsWith("...");
        assertThat(result.length()).isLessThan(text.length());

        // 验证截断后的文本不超过限制
        int truncatedTokens = TokenCounter.estimateTokens(result);
        assertThat(truncatedTokens).isLessThanOrEqualTo(12); // 允许一些误差
    }

    @Test
    void testTruncateToTokenLimit_EmptyText() {
        assertThat(TokenCounter.truncateToTokenLimit(null, 100)).isEmpty();
        assertThat(TokenCounter.truncateToTokenLimit("", 100)).isEmpty();
    }

    @Test
    void testTruncateToTokenLimit_ChineseText() {
        String text = "这是一个很长的中文文本。它包含多个句子，需要在合适的位置截断。" +
                      "我们希望能够在句号处截断，而不是在字符中间。";
        String result = TokenCounter.truncateToTokenLimit(text, 15);

        assertThat(result).isNotEqualTo(text);
        assertThat(result).endsWith("...");
        assertThat(TokenCounter.estimateTokens(result)).isLessThanOrEqualTo(18);
    }

    @Test
    void testTokenStats_ToString() {
        String text = "Hello 世界";
        TokenStats stats = TokenCounter.analyze(text);
        String str = stats.toString();

        assertThat(str).contains("tokens=");
        assertThat(str).contains("chars=");
        assertThat(str).contains("chinese=");
        assertThat(str).contains("words=");
        assertThat(str).contains("lines=");
    }

    @Test
    void testTokenStats_AvgTokensPerLine() {
        String text = "Hello World\nThis is a test\nAnother line";
        TokenStats stats = TokenCounter.analyze(text);

        double avg = stats.getAvgTokensPerLine();
        assertThat(avg).isGreaterThan(0);
        assertThat(avg).isEqualTo((double) stats.getEstimatedTokens() / stats.getLines());
    }

    @Test
    void testRealWorldExample_JavaDoc() {
        String javaDoc = "/**\n" +
                         " * 这是一个测试方法\n" +
                         " * This is a test method\n" +
                         " *\n" +
                         " * @param name 名称参数\n" +
                         " * @return 返回结果\n" +
                         " */";

        TokenStats stats = TokenCounter.analyze(javaDoc);

        assertThat(stats.getEstimatedTokens()).isGreaterThan(0);
        assertThat(stats.getLines()).isEqualTo(7);
        assertThat(stats.getChineseChars()).isGreaterThan(0);
        assertThat(stats.getEnglishWords()).isGreaterThan(0);

        System.out.println("Javadoc stats: " + stats);
    }

    @Test
    void testRealWorldExample_JavaCode() {
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

        TokenStats stats = TokenCounter.analyze(code);

        assertThat(stats.isProbablyCode()).isTrue();
        assertThat(stats.getCodeSymbols()).isGreaterThan(10);

        System.out.println("Code stats: " + stats);
    }

    @Test
    void testRealWorldExample_MixedContent() {
        String mixed = "这是一个使用 AI 生成 Javadoc 的插件。\n" +
                       "It supports multiple AI providers including QianWen and Ollama.\n" +
                       "代码示例：\n" +
                       "```java\n" +
                       "int tokens = TokenCounter.estimateTokens(text);\n" +
                       "```";

        TokenStats stats = TokenCounter.analyze(mixed);

        assertThat(stats.getChineseChars()).isGreaterThan(0);
        assertThat(stats.getEnglishWords()).isGreaterThan(0);
        assertThat(stats.getChineseRatio()).isBetween(0.2, 0.6);

        System.out.println("Mixed content stats: " + stats);
    }

    @Test
    void testPerformance_LargeText() {
        // 生成一个较大的文本
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是第 ").append(i).append(" 行测试文本。This is line ")
                .append(i).append(" test text.\n");
        }
        String largeText = sb.toString();

        long startTime = System.currentTimeMillis();
        TokenStats stats = TokenCounter.analyze(largeText);
        long endTime = System.currentTimeMillis();

        assertThat(stats.getLines()).isEqualTo(1000);
        assertThat(stats.getEstimatedTokens()).isGreaterThan(0);

        // 性能测试：处理 1000 行应该在 100ms 以内
        long duration = endTime - startTime;
        System.out.println("Large text processing time: " + duration + "ms");
        assertThat(duration).isLessThan(100);
    }
}

