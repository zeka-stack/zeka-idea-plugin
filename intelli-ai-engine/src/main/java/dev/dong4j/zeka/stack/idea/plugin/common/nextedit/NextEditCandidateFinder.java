package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 下一个编辑候选者查找器类
 * <p>用于在文本中查找与指定旧文本相似的候选替换位置, 支持基于编辑记录的上下文匹配, 适用于代码编辑器或文本对比工具中的智能替换建议功能.
 * <p>该类通过行级匹配, 相似度计算 (Levenshtein 距离) 和重叠检测, 确保候选位置不与当前编辑范围冲突, 并按相似度排序返回结果.
 * <p>支持精确匹配模式和模糊匹配模式, 可限制返回候选数量, 适用于编辑器中“查找替换”或“自动修复”等场景.
 * <p>使用示例:
 * <pre>{@code
 * NextEditCandidateFinder finder = new NextEditCandidateFinder();
 * List<NextEditCandidate> candidates = finder.findCandidates(fullText, editRecord, 5);
 * String debugInfo = finder.toDebugString(candidates);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditCandidateFinder {
    /** 最小相似度阈值, 用于判断候选编辑项的匹配质量 */
    private static final double MIN_SIMILARITY = 0.6;
    /** 最大行长度限制, 用于过滤过长的行内容 */
    private static final int MAX_LINE_LENGTH = 400;

    /**
     * 根据完整文本和编辑记录查找下一个编辑候选项
     * <p> 该方法通过分析完整文本与编辑记录中的旧文本, 查找可能的编辑候选位置, 并返回匹配度最高的候选项列表. 若旧文本为空或包含换行符, 则直接返回空列表. 若旧文本长度小于等于 2, 则调用精确匹配方法.
     *
     * @param fullText 完整文本内容, 不能为 null
     * @param edit     编辑记录对象, 包含旧文本和新文本信息, 不能为 null
     * @param limit    返回结果的最大数量
     * @return 匹配度最高的编辑候选项列表, 按匹配度降序排列
     */
    @NotNull
    List<NextEditCandidate> findCandidates(@NotNull String fullText,
                                           @NotNull NextEditRecord edit,
                                           int limit) {
        String oldText = edit.oldText();
        String newText = edit.newText();
        if (oldText.isBlank() || oldText.contains("\n")) {
            return List.of();
        }
        if (oldText.length() <= 2) {
            return findExactCandidates(fullText, edit, limit);
        }
        List<NextEditCandidate> results = new ArrayList<>();
        Set<Integer> seenStart = new HashSet<>();
        int lineStart = 0;
        int lineNumber = 0;
        int textLength = fullText.length();
        while (lineStart <= textLength) {
            int lineEnd = fullText.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = textLength;
            }
            int lineLength = lineEnd - lineStart;
            if (lineLength > 0 && lineLength <= MAX_LINE_LENGTH) {
                String line = fullText.substring(lineStart, lineEnd);
                NextEditCandidate candidate = bestMatchInLine(line, lineStart, lineNumber, oldText, newText, edit);
                if (candidate != null && seenStart.add(candidate.startIndex())) {
                    results.add(candidate);
                }
            }
            if (lineEnd == textLength) {
                break;
            }
            lineStart = lineEnd + 1;
            lineNumber++;
        }
        results.sort(Comparator.comparingDouble(NextEditCandidate::score).reversed());
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    /**
     * 将候选编辑项列表转换为调试字符串表示
     * <p> 该方法将候选编辑项列表中的每个项转换为 JSON 格式的字符串, 并将其拼接成一个多行字符串.
     * <p> 如果候选列表为空, 则返回字符串 "[]".
     *
     * @param candidates 候选编辑项列表, 不能为空
     * @return 调试字符串表示的候选编辑项列表
     */
    @NotNull
    String toDebugString(@NotNull List<NextEditCandidate> candidates) {
        if (candidates.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        for (NextEditCandidate candidate : candidates) {
            builder.append("{\"start_index\":").append(candidate.startIndex())
                .append(",\"end_index\":").append(candidate.endIndex())
                .append(",\"line\":").append(candidate.line())
                .append(",\"score\":").append(String.format(Locale.US, "%.2f", candidate.score()))
                .append(",\"preview\":\"").append(escapeJson(candidate.preview())).append("\"}");
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * 根据精确匹配查找编辑候选项
     * <p> 在给定的文本中查找与旧文本完全匹配的片段, 并返回对应的编辑候选项. 如果匹配项超过限制数量, 则只返回前 limit 个结果.
     *
     * @param fullText 完整的文本内容, 不能为 null
     * @param edit     编辑记录, 包含旧文本和新文本信息, 不能为 null
     * @param limit    返回的最大候选项数量
     * @return 匹配的编辑候选项列表
     */
    @NotNull
    private List<NextEditCandidate> findExactCandidates(@NotNull String fullText,
                                                        @NotNull NextEditRecord edit,
                                                        int limit) {
        String oldText = edit.oldText();
        String newText = edit.newText();
        List<NextEditCandidate> results = new ArrayList<>();
        int fromIndex = 0;
        int count = 0;
        while (count < limit) {
            int index = fullText.indexOf(oldText, fromIndex);
            if (index < 0) {
                break;
            }
            int end = index + oldText.length();
            if (!overlaps(index, end, edit.startOffset(), edit.endOffset())) {
                String candidateText = fullText.substring(index, end);
                if (!candidateText.equals(newText)) {
                    int line = lineNumberAt(fullText, index);
                    String preview = previewAt(fullText, index);
                    results.add(new NextEditCandidate(index, end, line, 1.0, preview));
                    count++;
                }
            }
            fromIndex = end;
        }
        return results;
    }

    /**
     * 在指定行中查找最佳匹配的编辑候选
     * <p> 遍历给定行的所有可能子字符串, 寻找与旧文本最相似且不与已有编辑区域重叠的候选位置.
     * <p> 仅当相似度大于等于 {@link NextEditCandidateFinder#MIN_SIMILARITY} 时返回有效候选.
     *
     * @param line            行内容 (未修剪)
     * @param lineStartOffset 该行在全文中的起始偏移量
     * @param lineNumber      行号 (从 0 开始计数)
     * @param oldText         被替换的旧文本
     * @param newText         新文本 (用于跳过完全相同的匹配)
     * @param edit            当前编辑记录, 用于检查是否与现有编辑区域重叠
     * @return 返回最佳匹配的编辑候选对象, 若无合适匹配则返回 null
     */
    private NextEditCandidate bestMatchInLine(@NotNull String line,
                                              int lineStartOffset,
                                              int lineNumber,
                                              @NotNull String oldText,
                                              @NotNull String newText,
                                              @NotNull NextEditRecord edit) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) {
            return null;
        }
        int oldLen = oldText.length();
        int bestStart = -1;
        int bestEnd = -1;
        double bestScore = 0.0;
        for (int i = 0; i + oldLen <= line.length(); i++) {
            int end = i + oldLen;
            String candidate = line.substring(i, end);
            if (candidate.equals(newText)) {
                continue;
            }
            double score = similarity(oldText, candidate);
            if (score > bestScore) {
                int globalStart = lineStartOffset + i;
                int globalEnd = lineStartOffset + end;
                if (!overlaps(globalStart, globalEnd, edit.startOffset(), edit.endOffset())) {
                    bestScore = score;
                    bestStart = globalStart;
                    bestEnd = globalEnd;
                }
            }
        }
        if (bestScore < MIN_SIMILARITY || bestStart < 0) {
            return null;
        }
        String preview = line;
        if (preview.length() > 120) {
            preview = preview.substring(0, 120);
        }
        return new NextEditCandidate(bestStart, bestEnd, lineNumber, bestScore, preview);
    }

    /**
     * 检查两个区间是否重叠
     * <p> 给定两个区间 [start, end) 和 [lastStart, lastEnd), 判断它们是否有重叠部分
     *
     * @param start     第一个区间的起始位置
     * @param end       第一个区间的结束位置
     * @param lastStart 第二个区间的起始位置
     * @param lastEnd   第二个区间的结束位置
     * @return 如果两个区间重叠, 则返回 true; 否则返回 false
     */
    private boolean overlaps(int start, int end, int lastStart, int lastEnd) {
        return start < lastEnd && end > lastStart;
    }

    /**
     * 计算两个字符串之间的相似度分数
     * <p>使用编辑距离 (Levenshtein 距离) 计算两个字符串的相似度, 相似度分数范围为 [0.0, 1.0], 值越接近 1.0 表示越相似
     * <p>相似度公式:1.0 - (编辑距离 / 两个字符串长度的最大值)
     * <p>当两个字符串长度均为 0 时, 返回 1.0
     *
     * @param a 第一个字符串, 不能为空
     * @param b 第二个字符串, 不能为空
     * @return 相似度分数, 范围为 [0.0, 1.0]
     */
    private double similarity(@NotNull String a, @NotNull String b) {
        int dist = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 1.0;
        }
        return 1.0 - ((double) dist / (double) max);
    }

    /**
     * 计算两个字符串之间的编辑距离 (Levenshtein 距离)
     * <p> 该算法用于衡量两个字符串之间的差异程度, 常用于拼写检查, 文本相似度比较等场景.
     * <p> 编辑距离定义为将一个字符串转换为另一个字符串所需的最少单字符编辑操作次数 (插入, 删除, 替换).
     * <p> 使用动态规划实现, 时间复杂度为 O(n*m), 空间复杂度为 O(min(n, m)).
     * <p> 示例:
     * <pre>{@code
     * int distance = levenshtein("kitten", "sitting"); // 返回 3
     * int distance2 = levenshtein("hello", "world");   // 返回 4
     * }</pre>
     *
     * @param a 第一个字符串, 不能为空
     * @param b 第二个字符串, 不能为空
     * @return 两个字符串之间的编辑距离, 非负整数
     */
    private int levenshtein(@NotNull String a, @NotNull String b) {
        int n = a.length();
        int m = b.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[m];
    }

    /**
     * 获取给定偏移量在文本中的行号
     * <p> 从文本的起始位置遍历到指定偏移量, 统计换行符的数量以确定行号
     *
     * @param text   输入的文本
     * @param offset 指定的偏移量
     * @return 行号, 从 0 开始计数
     */
    private int lineNumberAt(@NotNull String text, int offset) {
        int line = 0;
        int limit = Math.min(offset, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * 获取指定偏移位置所在行的预览文本
     * <p> 从指定偏移位置向上查找最近的换行符确定行首, 向下查找下一个换行符或文本末尾确定行尾, 截取该行内容并限制长度不超过 120 字符, 换行符替换为 "\\n"
     *
     * @param text   输入文本, 不能为 null
     * @param offset 偏移位置, 用于定位所在行
     * @return 该行的预览文本, 最多包含 120 个字符, 换行符替换为 "\\n"
     */
    @NotNull
    private String previewAt(@NotNull String text, int offset) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        int lineEnd = text.indexOf('\n', offset);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        String preview = text.substring(lineStart, lineEnd);
        if (preview.length() > 120) {
            preview = preview.substring(0, 120);
        }
        return preview.replace("\n", "\\n");
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     * <p> 将字符串中的反斜杠 ("\\") 替换为双反斜杠 ("\\\\"), 并将双引号 ("\"") 替换为转义后的双引号 ("\\\"")
     *
     * @param text 输入的字符串
     * @return 转义后的字符串
     */
    @NotNull
    private String escapeJson(@NotNull String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}
