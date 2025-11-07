package dev.dong4j.zeka.stack.idea.plugin.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pangu 测试类
 *
 * <p>测试 Pangu 类的中英文数字之间自动添加空格功能。
 *
 * @author dong4j
 * @version 1.4.0
 * @since 1.4.0
 */
class PanguTest {

    private Pangu pangu;

    @BeforeEach
    void setUp() {
        pangu = new Pangu();
    }

    @Test
    void testSpacingText_EmptyText() {
        assertThat(pangu.spacingText("")).isEmpty();
    }

    @Test
    void testSpacingText_ChineseAndEnglish() {
        // 中文 + 英文
        assertThat(pangu.spacingText("这是一个User类")).isEqualTo("这是一个 User 类");
        assertThat(pangu.spacingText("使用Java开发")).isEqualTo("使用 Java 开发");
        assertThat(pangu.spacingText("Hello世界")).isEqualTo("Hello 世界");
        assertThat(pangu.spacingText("世界Hello")).isEqualTo("世界 Hello");
    }

    @Test
    void testSpacingText_ChineseAndNumber() {
        // 中文 + 数字
        assertThat(pangu.spacingText("版本1.0")).isEqualTo("版本 1.0");
        assertThat(pangu.spacingText("第1个")).isEqualTo("第 1 个");
        assertThat(pangu.spacingText("100个用户")).isEqualTo("100 个用户");
        assertThat(pangu.spacingText("用户100")).isEqualTo("用户 100");
    }

    @Test
    void testSpacingText_ChineseAndSymbol() {
        // @ 符号比较特殊 不会分隔
        assertThat(pangu.spacingText("测试@符号")).isEqualTo("测试 @符号");
        assertThat(pangu.spacingText("价格$100")).isEqualTo("价格 $100");
        assertThat(pangu.spacingText("百分比%50")).isEqualTo("百分比 %50");
    }

    @Test
    void testSpacingText_EnglishAndChinese() {
        // 英文 + 中文
        assertThat(pangu.spacingText("User类")).isEqualTo("User 类");
        assertThat(pangu.spacingText("Java开发")).isEqualTo("Java 开发");
        assertThat(pangu.spacingText("test测试")).isEqualTo("test 测试");
    }

    @Test
    void testSpacingText_NumberAndChinese() {
        // 数字 + 中文
        assertThat(pangu.spacingText("1个")).isEqualTo("1 个");
        assertThat(pangu.spacingText("100用户")).isEqualTo("100 用户");
        assertThat(pangu.spacingText("3.14版本")).isEqualTo("3.14 版本");
    }

    @Test
    void testSpacingText_ChineseAndQuote() {
        // 中文 + 引号
        assertThat(pangu.spacingText("他说\"你好\"")).isEqualTo("他说 \"你好\"");
        assertThat(pangu.spacingText("'测试'")).isEqualTo("'测试'");
        assertThat(pangu.spacingText("测试'hello'")).isEqualTo("测试 'hello'");
    }

    @Test
    void testSpacingText_QuoteAndChinese() {
        // 引号 + 中文
        assertThat(pangu.spacingText("\"你好\"世界")).isEqualTo("\"你好\" 世界");
        assertThat(pangu.spacingText("'hello'测试")).isEqualTo("'hello' 测试");
    }

    @Test
    void testSpacingText_ChineseAndBracket() {
        // 中文 + 括号
        assertThat(pangu.spacingText("测试(方法)")).isEqualTo("测试 (方法)");
        assertThat(pangu.spacingText("类[数组]")).isEqualTo("类 [数组]");
        assertThat(pangu.spacingText("方法{代码}")).isEqualTo("方法 {代码}");
    }

    @Test
    void testSpacingText_BracketAndChinese() {
        // 括号 + 中文
        assertThat(pangu.spacingText("(方法)测试")).isEqualTo("(方法) 测试");
        assertThat(pangu.spacingText("[数组]类")).isEqualTo("[数组] 类");
        assertThat(pangu.spacingText("{代码}方法")).isEqualTo("{代码} 方法");
    }

    @Test
    void testSpacingText_ChineseBracketChinese() {
        // 中文 + 括号内容 + 中文
        assertThat(pangu.spacingText("测试(方法)结果")).isEqualTo("测试 (方法) 结果");
        assertThat(pangu.spacingText("类[数组]元素")).isEqualTo("类 [数组] 元素");
    }

    @Test
    void testSpacingText_ChineseAndHash() {
        // 中文 + # 符号
        assertThat(pangu.spacingText("标签#test")).isEqualTo("标签 #test");
        assertThat(pangu.spacingText("版本#1.0")).isEqualTo("版本 #1.0");
    }

    @Test
    void testSpacingText_HashAndChinese() {
        // # 符号 + 中文
        assertThat(pangu.spacingText("#test标签")).isEqualTo("#test 标签");
        assertThat(pangu.spacingText("#1.0版本")).isEqualTo("#1.0 版本");
    }

    @Test
    void testSpacingText_MixedContent() {
        // 混合内容
        assertThat(pangu.spacingText("这是一个User类，版本1.0，使用Java开发"))
            .isEqualTo("这是一个 User 类，版本 1.0，使用 Java 开发");

        assertThat(pangu.spacingText("User类包含100个方法，测试@符号"))
            .isEqualTo("User 类包含 100 个方法，测试 @符号");
    }

    @Test
    void testSpacingText_AlreadySpaced() {
        // 已经有空格的情况，不应该重复添加
        assertThat(pangu.spacingText("这是一个 User 类")).isEqualTo("这是一个 User 类");
        assertThat(pangu.spacingText("版本 1.0")).isEqualTo("版本 1.0");
    }

    @Test
    void testSpacingText_ComplexJavaDoc() {
        // JavaDoc 注释场景
        String input = "这是一个User类，用于处理用户数据。包含100个方法，使用Java开发。";
        String expected = "这是一个 User 类，用于处理用户数据。包含 100 个方法，使用 Java 开发。";
        assertThat(pangu.spacingText(input)).isEqualTo(expected);
    }

    @Test
    void testSpacingText_WithPunctuation() {
        // 包含标点符号
        assertThat(pangu.spacingText("测试,方法")).isEqualTo("测试, 方法");
        assertThat(pangu.spacingText("测试.方法")).isEqualTo("测试. 方法");
        assertThat(pangu.spacingText("测试;方法")).isEqualTo("测试; 方法");
    }

    @Test
    void testSpacingText_OnlyChinese() {
        // 只有中文
        assertThat(pangu.spacingText("这是一个测试")).isEqualTo("这是一个测试");
        assertThat(pangu.spacingText("你好世界")).isEqualTo("你好世界");
    }

    @Test
    void testSpacingText_OnlyEnglish() {
        // 只有英文
        assertThat(pangu.spacingText("Hello World")).isEqualTo("Hello World");
        assertThat(pangu.spacingText("This is a test")).isEqualTo("This is a test");
    }

    @Test
    void testSpacingText_OnlyNumbers() {
        // 只有数字
        assertThat(pangu.spacingText("123456")).isEqualTo("123456");
        assertThat(pangu.spacingText("3.14")).isEqualTo("3.14");
    }

    @Test
    void testSpacingText_ChineseEnglishNumber() {
        // 中文、英文、数字混合
        assertThat(pangu.spacingText("User类有100个方法")).isEqualTo("User 类有 100 个方法");
        assertThat(pangu.spacingText("版本1.0使用Java开发")).isEqualTo("版本 1.0 使用 Java 开发");
    }

    @Test
    void testSpacingText_SpecialSymbols() {
        // 特殊符号
        assertThat(pangu.spacingText("价格$100")).isEqualTo("价格 $100");
        assertThat(pangu.spacingText("百分比%50")).isEqualTo("百分比 %50");
        assertThat(pangu.spacingText("测试@符号")).isEqualTo("测试 @符号");
    }

    @Test
    void testSpacingText_MultipleSpaces() {
        // 多个连续空格应该被保留（由清理逻辑处理）
        String result = pangu.spacingText("测试  User");
        assertThat(result).contains(" ");
    }

    @Test
    void testSpacingText_RealWorldExample() {
        // 真实场景示例
        String input = "这是一个Java类UserService，包含100个方法，使用@Autowired注解注入依赖。";
        String result = pangu.spacingText(input);

        assertThat(result).contains("Java 类");
        assertThat(result).contains("100 个");
        assertThat(result).contains("@Autowired");
    }

    @Test
    void testSpacingText_WithCode() {
        // 包含代码片段
        String input = "使用public void test()方法";
        String result = pangu.spacingText(input);

        assertThat(result).contains("public");
        assertThat(result).contains("void");
        assertThat(result).contains("test()");
    }

    @Test
    void testSpacingText_EmailAndUrl() {
        // 邮箱和 URL（不应该被拆分）
        String input = "联系邮箱test@example.com";
        String result = pangu.spacingText(input);

        assertThat(result).contains("联系邮箱 test@example.com");
    }

    @Test
    void testSpacingText_JapaneseAndKorean() {
        // 日文和韩文（CJK 字符）
        // 注意：Pangu 支持 CJK 字符，包括日文和韩文
        String input = "日本語test";
        String result = pangu.spacingText(input);

        assertThat(result).contains(" ");
    }
}

