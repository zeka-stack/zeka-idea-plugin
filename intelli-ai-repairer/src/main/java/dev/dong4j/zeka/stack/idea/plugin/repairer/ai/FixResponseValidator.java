package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import java.util.List;

/**
 * FIX 响应验证工具类
 * <p>提供对响应文本的标准化处理功能, 主要用于去除代码块标记 (如 ```) 并提取有效内容.
 * 该类通过静态方法实现文本清洗逻辑, 适用于需要净化用户输入或解析结构化文本的场景.
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class FixResponseValidator {
    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 仅提供静态方法, 不允许外部创建实例
     */
    private FixResponseValidator() {
    }

    /**
     * 清洗并标准化输入的响应字符串
     * <p> 该方法会移除输入字符串中的代码块标记 (如 ```), 并尝试提取和清理代码块内部内容.
     * 如果处理后的内容仍包含特殊标记 (如 ```,<<<,>>>), 则返回空字符串.
     *
     * @param raw 原始输入字符串, 可能为 null
     * @return 处理后的标准化字符串, 若包含非法标记则返回空字符串; 若输入为 null 则返回空字符串
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int fenceStart = trimmed.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = trimmed.indexOf("```", fenceStart + 3);
            if (fenceEnd > fenceStart) {
                String inside = trimmed.substring(fenceStart + 3, fenceEnd);
                int firstLineBreak = inside.indexOf('\n');
                if (firstLineBreak >= 0) {
                    String firstLine = inside.substring(0, firstLineBreak).trim();
                    if (!firstLine.isEmpty() && firstLine.length() <= 10 && firstLine.matches("[a-zA-Z]+")) {
                        inside = inside.substring(firstLineBreak + 1);
                    }
                }
                trimmed = inside.trim();
            }
        }
        if (trimmed.contains("```") || trimmed.contains("<<<") || trimmed.contains(">>>")) {
            return "";
        }
        return trimmed;
    }

    /**
     * 提取统一 diff 文本
     *
     * @param raw AI 返回文本
     * @return 统一 diff 内容，若不合法则返回空字符串
     */
    public static String extractUnifiedDiff(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int diffStart = indexOfDiffStart(trimmed);
        if (diffStart < 0) {
            return "";
        }
        String diff = trimmed.substring(diffStart).trim();
        if (!diff.contains("@@")) {
            return "";
        }
        if (diff.contains("```") || diff.contains("<<<") || diff.contains(">>>")) {
            return "";
        }
        return diff;
    }

    /**
     * 将统一 diff 应用到原始片段
     *
     * @param original 原始代码片段
     * @param diff     统一 diff
     * @return 修复后的片段，失败返回空字符串
     */
    public static String applyUnifiedDiffToSnippet(String original, String diff) {
        if (original == null || diff == null || diff.isBlank()) {
            return "";
        }
        List<String> originalLines = List.of(original.split("\n", -1));
        List<String> result = new java.util.ArrayList<>();

        int index = 0;
        String[] lines = diff.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.startsWith("diff --git") || line.startsWith("---") || line.startsWith("+++")) {
                i++;
                continue;
            }
            if (!line.startsWith("@@")) {
                i++;
                continue;
            }
            Hunk hunk = parseHunkHeader(line);
            if (hunk == null) {
                return "";
            }
            int hunkStart = Math.max(0, hunk.oldStart - 1);
            if (hunkStart < index || hunkStart > originalLines.size()) {
                return "";
            }
            while (index < hunkStart) {
                result.add(originalLines.get(index));
                index++;
            }
            i++;
            while (i < lines.length && !lines[i].startsWith("@@")) {
                String hunkLine = lines[i];
                if (hunkLine.startsWith("\\ No newline at end of file")) {
                    i++;
                    continue;
                }
                if (hunkLine.isEmpty()) {
                    hunkLine = " ";
                }
                char prefix = hunkLine.charAt(0);
                String content = hunkLine.length() > 1 ? hunkLine.substring(1) : "";
                if (prefix == ' ') {
                    if (index >= originalLines.size() || !originalLines.get(index).equals(content)) {
                        return "";
                    }
                    result.add(originalLines.get(index));
                    index++;
                } else if (prefix == '-') {
                    if (index >= originalLines.size() || !originalLines.get(index).equals(content)) {
                        return "";
                    }
                    index++;
                } else if (prefix == '+') {
                    result.add(content);
                } else {
                    // unexpected line
                    return "";
                }
                i++;
            }
        }
        while (index < originalLines.size()) {
            result.add(originalLines.get(index));
            index++;
        }
        return String.join("\n", result);
    }

    /**
     * 查找统一 diff 文本的起始位置
     * <p> 在输入文本中搜索以下三种标记的首次出现位置:`diff--git`,`---` 或 `@@ `, 并返回其起始索引.
     * 若未找到任何标记, 则返回 - 1.
     *
     * @param text 输入的文本字符串
     * @return 首个匹配标记的起始索引, 若未找到则返回 - 1
     */
    private static int indexOfDiffStart(String text) {
        int idx = text.indexOf("diff --git");
        if (idx >= 0) {
            return idx;
        }
        idx = text.indexOf("--- ");
        if (idx >= 0) {
            return idx;
        }
        idx = text.indexOf("@@ ");
        if (idx >= 0) {
            return idx;
        }
        return -1;
    }

    /**
     * 解析统一 diff 的块头行, 提取旧文件起始行号
     * <p> 从输入的 diff 行中查找 '-','+' 和 '@@' 标记, 提取旧文件起始行号并封装为 Hunk 对象. 若格式不合法则返回 null
     *
     * @param line 待解析的 diff 块头行, 例如 "@@ -10,5 +12,3 @@"
     * @return 解析成功则返回包含起始行号的 Hunk 对象, 解析失败或格式不合法则返回 null
     */
    private static Hunk parseHunkHeader(String line) {
        // @@ -a,b +c,d @@
        int minus = line.indexOf('-');
        int plus = line.indexOf('+');
        int at = line.indexOf("@@", 2);
        if (minus < 0 || plus < 0 || at < 0) {
            return null;
        }
        String oldPart = line.substring(minus + 1, plus - 1).trim();
        String[] oldParts = oldPart.split(",");
        try {
            int oldStart = Integer.parseInt(oldParts[0]);
            return new Hunk(oldStart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 补丁块元数据记录类
     * <p> 用于封装统一 diff 格式中每个补丁块的起始行号信息, 作为解析和应用 diff 时的结构化数据载体.
     * 该类为不可变数据记录 (record), 仅包含一个字段 {@code oldStart}, 表示原始文件中该补丁块所对应的起始行号 (从 1 开始计数).
     * 适用于 diff 解析器在处理统一 diff 文本时, 对每个补丁块进行结构化封装, 便于后续的行号映射和内容替换操作.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.02.02
     * @since 1.0.0
     */
    private record Hunk(int oldStart) {
    }
}
