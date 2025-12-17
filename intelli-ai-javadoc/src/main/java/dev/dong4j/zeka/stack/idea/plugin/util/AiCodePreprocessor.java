package dev.dong4j.zeka.stack.idea.plugin.util;

import org.jetbrains.annotations.NotNull;

/**
 * AI 代码预处理器
 * <p>
 * 提供代码预处理功能, 用于清理和格式化源代码, 包括移除注释,
 * 消除多余空格, 标准化缩进等操作, 为 AI 代码分析和处理提供标准化的代码输入
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AiCodePreprocessor {

    /**
     * 处理 Java 源码，用于 AI 注释生成
     *
     * <p>处理流程：
     * <ol>
     *   <li>根据参数决定是否删除所有注释（Javadoc、块注释、单行注释）</li>
     *   <li>删除多余空格和空行</li>
     *   <li>压缩缩进到最小层级（每层 1 个空格）</li>
     * </ol>
     *
     * <p>注意事项：
     * <ul>
     *   <li>保持代码的层级关系，确保 AI 能正确理解代码结构</li>
     *   <li>删除注释可能会丢失一些上下文信息，但能显著减少 token 使用</li>
     *   <li>压缩后的代码仍然保持基本的可读性</li>
     * </ul>
     *
     * @param code           原始 Java 代码
     * @param removeComments 是否删除注释
     * @return 处理后的代码
     */
    @NotNull
    public static String preprocess(@NotNull String code, boolean removeComments) {
        if (code.isEmpty()) {
            return "";
        }

        final String noExtraLines = deleteString(code, removeComments);

        // 4. 缩进压缩到最小层级
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;

        for (String line : noExtraLines.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 检测缩进层级
            if (line.endsWith("}")) {
                indentLevel = Math.max(indentLevel - 1, 0);
            }

            // 添加最小缩进（每层 1 个空格）
            result.append(" ".repeat(Math.max(0, indentLevel)));
            result.append(line).append("\n");

            if (line.endsWith("{")) {
                indentLevel++;
            }
        }

        return result.toString().trim();
    }

    /**
     * 删除代码中的注释并清理多余空格
     * <p> 根据是否移除注释的参数, 从代码中删除所有注释, 并清理多余的空白字符和格式
     *
     * @param code           要处理的原始代码字符串
     * @param removeComments 是否移除注释, 为 true 时删除所有注释
     * @return 处理后的代码字符串, 已移除注释并清理了多余空格
     */
    @NotNull
    private static String deleteString(@NotNull String code, boolean removeComments) {
        String processedCode = code;

        // 1. 根据参数决定是否删除所有注释
        if (removeComments) {
            processedCode = code
                .replaceAll("(?s)/\\*\\*.*?\\*/", "")   // Javadoc
                .replaceAll("(?s)/\\*.*?\\*/", "")      // 块注释
                .replaceAll("//.*", "");                // 单行注释
        }

        // 2. 删除多余空格
        String noExtraSpaces = processedCode
            .replaceAll("[ \\t]+", " ")            // 多空格合并为单空格
            .replaceAll(" ?([{}();,=<>+*/-]) ?", "$1"); // 删除符号两侧空格

        // 3. 删除多余空行
        return noExtraSpaces
            .replaceAll("(?m)^[ \\t]*\\r?\\n", "") // 删除空行
            .trim();
    }

    /**
     * 处理 Java 源码，用于 AI 注释生成（默认删除注释）
     *
     * @param code 原始 Java 代码
     * @return 处理后的代码
     * @see #preprocess(String, boolean)
     */
    @NotNull
    public static String preprocess(@NotNull String code) {
        return preprocess(code, false);
    }
}

