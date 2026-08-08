package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

/**
 * 二进制文件变更摘要工具
 * <p>
 * 向 AI prompt 拼接「仅路径、无内容」的英文变更列表。文案直接使用英文常量，不做本地化转换。
 *
 * @author dong4j
 * @version 2026.2.1002
 * @since 2026.2.1002
 */
public final class BinaryFileChangeSummary {

    private BinaryFileChangeSummary() {
    }

    /**
     * 将二进制变更以英文路径列表追加到摘要中。
     *
     * @param summary      目标摘要缓冲
     * @param diffs        全部 CodeDiff（会过滤 {@code binary == true}）
     * @param maxDiffChars 最大字符数
     */
    public static void append(@NotNull StringBuilder summary,
                              @NotNull List<CodeDiff> diffs,
                              int maxDiffChars) {
        boolean hasBinary = false;
        for (CodeDiff diff : diffs) {
            if (diff.binary) {
                hasBinary = true;
                break;
            }
        }
        if (!hasBinary) {
            return;
        }

        // 英文直接拼接，不做 i18n 转换
        summary.append("Binary file changes (paths only, no content):\n");
        for (CodeDiff diff : diffs) {
            if (summary.length() >= maxDiffChars) {
                break;
            }
            if (!diff.binary) {
                continue;
            }
            summary.append("- ")
                .append(label(diff.changeType))
                .append(": ")
                .append(diff.filePath)
                .append("\n");
        }
        summary.append("\n");
    }

    /**
     * 变更类型对应的英文标签（固定英文，不做转换）。
     *
     * @param changeType 变更类型
     * @return Added / Deleted / Modified / Renamed / Moved
     */
    @NotNull
    public static String label(@NotNull CodeDiff.ChangeType changeType) {
        return switch (changeType) {
            case ADD -> "Added";
            case DELETE -> "Deleted";
            case MODIFY -> "Modified";
            case RENAME -> "Renamed";
            case MOVE -> "Moved";
        };
    }
}
