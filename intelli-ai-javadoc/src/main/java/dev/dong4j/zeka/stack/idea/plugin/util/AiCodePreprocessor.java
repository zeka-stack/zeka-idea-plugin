package dev.dong4j.zeka.stack.idea.plugin.util;

import org.jetbrains.annotations.NotNull;

/**
 * AI 代码预处理器
 *
 * <p>处理 Java 源码，用于 AI 注释生成：
 * <ul>
 *   <li>删除所有注释</li>
 *   <li>删除多余空格和空行</li>
 *   <li>缩进压缩到最小层级（每层 1 个空格）</li>
 * </ul>
 *
 * <p>设计目标：
 * <ul>
 *   <li>减少 token 使用量</li>
 *   <li>保持代码层级关系，避免 AI 理解错误</li>
 *   <li>确保代码结构清晰可读</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class AiCodePreprocessor {

    /**
     * 处理 Java 源码，用于 AI 注释生成
     *
     * <p>处理流程：
     * <ol>
     *   <li>删除所有注释（Javadoc、块注释、单行注释）</li>
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
     * @param code 原始 Java 代码
     * @return 处理后的代码
     */
    @NotNull
    public static String preprocess(@NotNull String code) {
        if (code.isEmpty()) {
            return "";
        }

        // 1. 删除所有注释
        String noComments = code
            .replaceAll("(?s)/\\*\\*.*?\\*/", "")   // Javadoc
            .replaceAll("(?s)/\\*.*?\\*/", "")      // 块注释
            .replaceAll("//.*", "");                // 单行注释

        // 2. 删除多余空格
        String noExtraSpaces = noComments
            .replaceAll("[ \\t]+", " ")            // 多空格合并为单空格
            .replaceAll(" ?([{}();,=<>+*/-]) ?", "$1"); // 删除符号两侧空格

        // 3. 删除多余空行
        String noExtraLines = noExtraSpaces
            .replaceAll("(?m)^[ \\t]*\\r?\\n", "") // 删除空行
            .trim();

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
}

