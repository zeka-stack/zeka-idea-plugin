package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

/**
 * 统一 diff 解析器
 * <p>
 * 将 {@code git diff} / {@code diff --git} 风格的 unified diff 文本解析为 {@link CodeDiff} 列表，
 * 供提交信息生成与结构化上下文构建使用。
 */
public final class UnifiedDiffParser {

    private static final Pattern DIFF_GIT_PATTERN = Pattern.compile("^diff --git a/(.+) b/(.+)$");

    private UnifiedDiffParser() {
        // 工具类，禁止实例化
    }

    @NotNull
    public static List<CodeDiff> parseToCodeDiffs(@NotNull String diffText) {
        List<CodeDiff> diffs = new ArrayList<>();
        if (diffText.isBlank()) {
            return diffs;
        }

        String[] lines = diffText.split("\n", -1);

        String currentPath = null;
        StringBuilder currentBlock = null;
        int added = 0;
        int deleted = 0;
        boolean newFile = false;
        boolean deletedFile = false;
        boolean renamed = false;

        for (String line : lines) {
            if (line.startsWith("diff --git ")) {
                flushParsedDiff(diffs, currentPath, currentBlock, added, deleted, newFile, deletedFile, renamed);

                currentPath = null;
                currentBlock = new StringBuilder();
                currentBlock.append(line).append("\n");
                added = 0;
                deleted = 0;
                newFile = false;
                deletedFile = false;
                renamed = false;

                Matcher matcher = DIFF_GIT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String beforePath = matcher.group(1);
                    String afterPath = matcher.group(2);
                    currentPath = resolveDiffPath(beforePath, afterPath);
                }
                continue;
            }

            if (currentBlock == null) {
                continue;
            }

            currentBlock.append(line).append("\n");

            if (line.startsWith("new file mode")) {
                newFile = true;
            } else if (line.startsWith("deleted file mode")) {
                deletedFile = true;
            } else if (line.startsWith("rename from") || line.startsWith("rename to")) {
                renamed = true;
            }

            if (line.startsWith("+") && !line.startsWith("+++")) {
                added++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                deleted++;
            }
        }

        flushParsedDiff(diffs, currentPath, currentBlock, added, deleted, newFile, deletedFile, renamed);
        return diffs;
    }

    private static void flushParsedDiff(@NotNull List<CodeDiff> diffs,
                                        @Nullable String filePath,
                                        @Nullable StringBuilder diffBlock,
                                        int added,
                                        int deleted,
                                        boolean newFile,
                                        boolean deletedFile,
                                        boolean renamed) {
        if (filePath == null || filePath.isBlank() || diffBlock == null || diffBlock.isEmpty()) {
            return;
        }
        CodeDiff.ChangeType changeType;
        if (deletedFile) {
            changeType = CodeDiff.ChangeType.DELETE;
        } else if (newFile) {
            changeType = CodeDiff.ChangeType.ADD;
        } else if (renamed) {
            changeType = CodeDiff.ChangeType.RENAME;
        } else {
            changeType = CodeDiff.ChangeType.MODIFY;
        }
        diffs.add(new CodeDiff(filePath, changeType, added, deleted, diffBlock.toString().trim(), null, null));
    }

    @NotNull
    private static String resolveDiffPath(@Nullable String beforePath, @Nullable String afterPath) {
        String path = afterPath != null && !afterPath.isBlank() && !"/dev/null".equals(afterPath) ? afterPath : beforePath;
        if (path == null) {
            return "";
        }
        return path.replace('\\', '/');
    }
}

