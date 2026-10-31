package dev.dong4j.zeka.stack.idea.plugin.kit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 盘古类 - 中英文混排格式化工具
 * <p>
 * 该类提供中英文混排文本的自动格式化功能, 通过在中文字符与英文 / 数字 / 符号之间添加适当的空格,
 * 以及处理引号, 括号, 井号等特殊符号的间距, 使中英文混排文本更加美观易读.
 * 主要包含文本间距调整和文件批量处理两种功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class Pangu {
    /**
     * 构造函数，用于初始化 Pangu 类的实例
     * <p>
     * 默认构造函数，不执行任何初始化操作
     */
    public Pangu() {
    }

    /*
     * Some capturing group patterns for convenience.
     *
     * CJK: Chinese, Japanese, Korean
     * ANS: Alphabet, Number, Symbol
     */
    /** CJK_ANS 模式用于匹配中日韩字符与英文字符的组合，支持不区分大小写 */
    private static final Pattern CJK_ANS = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])" +
        "([a-z0-9`~@\\$%\\^&\\*\\-_\\+=\\|\\\\/])",
        Pattern.CASE_INSENSITIVE
                                                          );
    /** 匹配 ASCII 字符与 CJK 字符相邻的模式，用于检测混合文本 */
    private static final Pattern ANS_CJK = Pattern.compile(
        "([a-z0-9`~!\\$%\\^&\\*\\-_\\+=\\|\\\\;:,\\./\\?])" +
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])",
        Pattern.CASE_INSENSITIVE
                                                          );

    /**
     * 用于匹配 CJK 字符与引号组合的正则表达式模式
     * <p>
     * 该模式用于识别日文假名、注音符号、CJK 统一汉字等字符后紧跟的引号
     *
     * @see java.util.regex.Pattern
     */
    private static final Pattern CJK_QUOTE = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])" +
        "([\"'])"
                                                            );
    /** CJK 引号模式，用于匹配中文字符环境下的引号符号 */
    private static final Pattern QUOTE_CJK = Pattern.compile(
        "([\"'])" +
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])"
                                                            );
    /** 用于匹配并提取字符串中的引号包裹内容的正则表达式模式 */
    private static final Pattern FIX_QUOTE = Pattern.compile("([\"'])(\\s*)(.+?)(\\s*)([\"'])");

    /** 用于匹配中日韩标点符号的正则表达式模式 */
    private static final Pattern CJK_BRACKET_CJK = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])" +
        "([\\({\\[]+(.*?)[\\)}\\]]+)" +
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])"
                                                                  );
    /**
     * 用于匹配中日韩文字与括号符号组合的正则表达式模式
     * <p>
     * 该模式用于识别中日韩文字后紧跟的括号、大括号、方括号或尖括号
     *
     * @see java.util.regex.Pattern
     */
    private static final Pattern CJK_BRACKET = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])" +
        "([\\(\\){}\\[\\]<>])"
                                                              );
    /**
     * 匹配括号字符与中日韩字符的正则表达式模式
     * <p>
     * 用于识别括号字符与中日韩字符的组合
     * <p>
     *
     * @see Pattern
     */
    private static final Pattern BRACKET_CJK = Pattern.compile(
        "([\\(\\){}\\[\\]<>])" +
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])"
                                                              );
    /** 用于匹配括号及其内部内容的正则表达式模式 */
    private static final Pattern FIX_BRACKET = Pattern.compile("([(\\({\\[)]+)(\\s*)(.+?)(\\s*)([\\)}\\]]+)");

    /** CJK 语言中的哈希符号匹配模式，用于识别包含哈希符号的标记 */
    private static final Pattern CJK_HASH = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])" +
        "(#(\\S+))"
                                                           );
    /**
     * 用于匹配包含哈希标签的 CJK 字符的正则表达式模式
     * <p>
     * 该模式用于识别格式为 "文本#CJK字符" 的字符串，其中 CJK 字符包括日文假名、注音符号、
     * CJK 兼容汉字和统一汉字等。
     *
     * @see Pattern
     */
    private static final Pattern HASH_CJK = Pattern.compile(
        "((\\S+)#)" +
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])"
                                                           );

    /** 匹配数字或中日韩字符与连字符之间的格式, 用于标准化范围表达式如 "1-10" 或 "一 - 十" 的空格处理 */
    private static final Pattern CJK_HYPHEN_CJK_OR_DIGIT = Pattern.compile(
        "([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])[ \\t]*-[ \\t]*" +
        "([\\d\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs}])"
                                                                          );
    private static final Pattern DIGIT_HYPHEN_CJK = Pattern.compile(
        "([\\d])[ \\t]*-[ \\t]*([\\p{InHiragana}\\p{InKatakana}\\p{InBopomofo}\\p{InCJKCompatibilityIdeographs}\\p{InCJKUnifiedIdeographs" +
        "}])"
                                                                   );

    /**
     * 对文本进行格式化处理，包括替换引号、括号和符号的空格格式
     * <p>
     * 该方法通过正则表达式匹配并替换文本中的特殊字符，如引号、括号和符号，以实现格式化处理。
     *
     * @param text 需要处理的原始文本
     * @return 格式化处理后的文本
     */
    public String spacingText(String text) {
        // CJK and quotes
        Matcher cqMatcher = CJK_QUOTE.matcher(text);
        text = cqMatcher.replaceAll("$1 $2");

        Matcher qcMatcher = QUOTE_CJK.matcher(text);
        text = qcMatcher.replaceAll("$1 $2");

        Matcher fixQuoteMatcher = FIX_QUOTE.matcher(text);
        text = fixQuoteMatcher.replaceAll("$1$3$5");

        // CJK and brackets
        String oldText = text;
        Matcher cbcMatcher = CJK_BRACKET_CJK.matcher(text);
        String newText = cbcMatcher.replaceAll("$1 $2 $4");
        text = newText;

        if (oldText.equals(newText)) {
            Matcher cbMatcher = CJK_BRACKET.matcher(text);
            text = cbMatcher.replaceAll("$1 $2");

            Matcher bcMatcher = BRACKET_CJK.matcher(text);
            text = bcMatcher.replaceAll("$1 $2");
        }

        Matcher fixBracketMatcher = FIX_BRACKET.matcher(text);
        text = fixBracketMatcher.replaceAll("$1$3$5");

        // CJK and hash
        Matcher chMatcher = CJK_HASH.matcher(text);
        text = chMatcher.replaceAll("$1 $2");

        Matcher hcMatcher = HASH_CJK.matcher(text);
        text = hcMatcher.replaceAll("$1 $3");

        // CJK and ANS
        Matcher caMatcher = CJK_ANS.matcher(text);
        text = caMatcher.replaceAll("$1 $2");

        Matcher acMatcher = ANS_CJK.matcher(text);
        text = acMatcher.replaceAll("$1 $2");

        Matcher cjkHyphenMatcher = CJK_HYPHEN_CJK_OR_DIGIT.matcher(text);
        text = cjkHyphenMatcher.replaceAll("$1 - $2");

        Matcher digitCjkHyphenMatcher = DIGIT_HYPHEN_CJK.matcher(text);
        text = digitCjkHyphenMatcher.replaceAll("$1 - $2");

        return text;
    }

    /**
     * 对输入文件中的每一行文本进行空格处理，并将结果写入输出文件
     * <p>
     * 该方法读取输入文件的内容，逐行处理空格，然后将处理后的文本写入输出文件。
     *
     * @param inputFile  需要处理的输入文件
     * @param outputFile 处理后写入的输出文件
     * @throws IOException 如果文件读取或写入过程中发生异常
     */
    public void spacingFile(File inputFile, File outputFile) throws IOException {
        // TODO: support charset

        FileReader fr = new FileReader(inputFile);
        BufferedReader br = new BufferedReader(fr);

        outputFile.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(outputFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        try {
            String line = br.readLine(); // readLine() do not contain newline char

            while (line != null) {
                line = spacingText(line);

                // TODO: keep file's raw newline char from difference OS platform
                bw.write(line);
                bw.newLine();

                line = br.readLine();
            }
        } finally {
            br.close();

            // 避免 writer 沒有實際操作就 close()，產生一個空檔案
            if (bw != null) {
                bw.close();
            }
        }
    }
}
