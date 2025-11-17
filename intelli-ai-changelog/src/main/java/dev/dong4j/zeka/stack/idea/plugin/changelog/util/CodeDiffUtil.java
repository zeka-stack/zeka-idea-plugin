package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

/**
 * 代码 Diff 工具类
 * <p>
 * 用于从 IntelliJ Platform 的 Change 对象中提取代码变更信息。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CodeDiffUtil {

    /**
     * 私有构造函数, 用于防止外部实例化
     * <p>
     * 该构造函数为私有, 确保 CodeDiffUtil 类只能通过静态方法或内部工厂方法创建实例
     */
    private CodeDiffUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 从 Change 集合中提取代码变更信息
     *
     * @param changes 变更集合
     * @return 代码变更信息列表
     */
    @NotNull
    public static List<CodeDiff> extractCodeDiffs(@NotNull Collection<Change> changes) {
        List<CodeDiff> codeDiffs = new ArrayList<>();

        for (Change change : changes) {
            CodeDiff codeDiff = extractCodeDiff(change);
            if (codeDiff != null) {
                codeDiffs.add(codeDiff);
            }
        }

        return codeDiffs;
    }

    /**
     * 从单个 Change 对象中提取代码变更信息
     *
     * @param change 变更对象
     * @return 代码变更信息，如果无法提取则返回 null
     */
    @Nullable
    public static CodeDiff extractCodeDiff(@NotNull Change change) {
        VirtualFile virtualFile = change.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        String filePath = virtualFile.getPath();
        CodeDiff.ChangeType changeType = determineChangeType(change);
        String diffContent = extractDiffContent(change);

        // 计算新增和删除的行数
        int addedLines = 0;
        int deletedLines = 0;
        if (diffContent != null) {
            String[] lines = diffContent.split("\n");
            for (String line : lines) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    addedLines++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    deletedLines++;
                }
            }
        }

        return new CodeDiff(filePath, changeType, addedLines, deletedLines, diffContent);
    }

    /**
     * 确定变更类型
     *
     * @param change 变更对象
     * @return 变更类型
     */
    @NotNull
    private static CodeDiff.ChangeType determineChangeType(@NotNull Change change) {
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        if (beforeRevision == null && afterRevision != null) {
            return CodeDiff.ChangeType.ADD;
        } else if (beforeRevision != null && afterRevision == null) {
            return CodeDiff.ChangeType.DELETE;
        } else if (beforeRevision != null && afterRevision != null) {
            // 检查是否是重命名或移动
            String beforePath = beforeRevision.getFile().getPath();
            String afterPath = afterRevision.getFile().getPath();
            if (!beforePath.equals(afterPath)) {
                return CodeDiff.ChangeType.RENAME;
            }
            return CodeDiff.ChangeType.MODIFY;
        }

        return CodeDiff.ChangeType.MODIFY;
    }

    /**
     * 提取 Diff 内容
     *
     * @param change 变更对象
     * @return Diff 内容字符串
     */
    @Nullable
    private static String extractDiffContent(@NotNull Change change) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            try {
                ContentRevision beforeRevision = change.getBeforeRevision();
                ContentRevision afterRevision = change.getAfterRevision();

                if (beforeRevision == null && afterRevision == null) {
                    return null;
                }

                String beforeContent = beforeRevision != null ? beforeRevision.getContent() : "";
                String afterContent = afterRevision != null ? afterRevision.getContent() : "";

                if (beforeContent == null) {
                    beforeContent = "";
                }
                if (afterContent == null) {
                    afterContent = "";
                }

                // 如果内容相同，返回 null
                if (beforeContent.equals(afterContent)) {
                    return null;
                }

                // 生成简单的 unified diff 格式
                return generateUnifiedDiff(
                    beforeRevision != null ? beforeRevision.getFile().getName() : "null",
                    afterRevision != null ? afterRevision.getFile().getName() : "null",
                    beforeContent,
                    afterContent
                                          );
            } catch (Exception e) {
                // 忽略异常，返回 null
                return null;
            }
        });
    }

    /**
     * 生成 Unified Diff 格式的字符串
     *
     * @param beforeFileName 修改前的文件名
     * @param afterFileName  修改后的文件名
     * @param beforeContent  修改前的内容
     * @param afterContent   修改后的内容
     * @return Unified Diff 格式的字符串
     */
    @NotNull
    private static String generateUnifiedDiff(@NotNull String beforeFileName,
                                              @NotNull String afterFileName,
                                              @NotNull String beforeContent,
                                              @NotNull String afterContent) {
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(beforeFileName).append("\n");
        diff.append("+++ ").append(afterFileName).append("\n");
        diff.append("@@ -1,").append(beforeContent.split("\n").length)
            .append(" +1,").append(afterContent.split("\n").length).append(" @@\n");

        String[] beforeLines = beforeContent.split("\n", -1);
        String[] afterLines = afterContent.split("\n", -1);

        int beforeIndex = 0;
        int afterIndex = 0;

        while (beforeIndex < beforeLines.length || afterIndex < afterLines.length) {
            if (beforeIndex >= beforeLines.length) {
                // 只有新增的行
                diff.append("+").append(afterLines[afterIndex]).append("\n");
                afterIndex++;
            } else if (afterIndex >= afterLines.length) {
                // 只有删除的行
                diff.append("-").append(beforeLines[beforeIndex]).append("\n");
                beforeIndex++;
            } else if (beforeLines[beforeIndex].equals(afterLines[afterIndex])) {
                // 相同的行
                diff.append(" ").append(beforeLines[beforeIndex]).append("\n");
                beforeIndex++;
                afterIndex++;
            } else {
                // 不同的行，先删除后添加
                diff.append("-").append(beforeLines[beforeIndex]).append("\n");
                beforeIndex++;
                diff.append("+").append(afterLines[afterIndex]).append("\n");
                afterIndex++;
            }
        }

        return diff.toString();
    }
}

