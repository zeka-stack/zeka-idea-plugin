package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

/**
 * 代码差异工具类
 * <p>
 * 提供代码变更差异提取和分析功能, 用于处理版本控制系统中的文件变更,
 * 可以从变更对象中提取代码差异信息, 包括变更类型, 新增行数, 删除行数和差异内容等
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
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
        } else if (beforeRevision != null) {
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
    @SuppressWarnings("D")
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
                String diff = generateUnifiedDiff(
                    beforeRevision != null ? beforeRevision.getFile().getName() : "null",
                    afterRevision != null ? afterRevision.getFile().getName() : "null",
                    beforeContent,
                    afterContent,
                    change.getVirtualFile()
                                          );
                return diff.isEmpty() ? null : diff;
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
                                              @NotNull String afterContent,
                                              @Nullable VirtualFile virtualFile) {
        List<LineFragment> fragments = ComparisonManager.getInstance()
            .compareLines(beforeContent, afterContent, ComparisonPolicy.DEFAULT,
                          ProgressIndicatorProvider.getGlobalProgressIndicator());
        if (fragments.isEmpty()) {
            return "";
        }

        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(beforeFileName).append("\n");
        diff.append("+++ ").append(afterFileName).append("\n");
        boolean hasChanges = false;

        String[] beforeLines = beforeContent.split("\n", -1);
        String[] afterLines = afterContent.split("\n", -1);

        for (LineFragment fragment : fragments) {
            int beforeStart = fragment.getStartLine1();
            int beforeEnd = fragment.getEndLine1();
            int afterStart = fragment.getStartLine2();
            int afterEnd = fragment.getEndLine2();
            List<String> beforeChanged = extractLines(beforeLines, beforeStart, beforeEnd);
            List<String> afterChanged = extractLines(afterLines, afterStart, afterEnd);
            if (isWhitespaceOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            if (isImportOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            List<String> beforeOutput = filterNonIgnorableLines(beforeChanged);
            List<String> afterOutput = filterNonIgnorableLines(afterChanged);
            if (beforeOutput.isEmpty() && afterOutput.isEmpty()) {
                continue;
            }
            hasChanges = true;
            diff.append("@@ -").append(beforeStart + 1).append(",").append(beforeEnd - beforeStart)
                .append(" +").append(afterStart + 1).append(",").append(afterEnd - afterStart)
                .append(" @@\n");

            String context = resolveJavaSymbolContext(virtualFile, afterStart, beforeStart);
            if (context != null && !context.isEmpty()) {
                diff.append("上下文: ").append(context).append("\n");
            }
            for (String line : beforeOutput) {
                diff.append("-").append(line).append("\n");
            }
            for (String line : afterOutput) {
                diff.append("+").append(line).append("\n");
            }
        }

        return hasChanges ? diff.toString() : "";
    }

    @NotNull
    private static List<String> extractLines(@NotNull String[] lines, int start, int end) {
        List<String> result = new ArrayList<>();
        for (int i = start; i < end && i < lines.length; i++) {
            result.add(lines[i]);
        }
        return result;
    }

    @NotNull
    private static List<String> filterNonIgnorableLines(@NotNull List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (!isIgnorableLine(line)) {
                result.add(line);
            }
        }
        return result;
    }

    private static boolean isWhitespaceOnlyChange(@NotNull List<String> beforeLines,
                                                  @NotNull List<String> afterLines) {
        if (beforeLines.size() != afterLines.size()) {
            return false;
        }
        for (int i = 0; i < beforeLines.size(); i++) {
            if (!normalizeLine(beforeLines.get(i)).equals(normalizeLine(afterLines.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isImportOnlyChange(@NotNull List<String> beforeLines,
                                              @NotNull List<String> afterLines) {
        if (beforeLines.isEmpty() && afterLines.isEmpty()) {
            return false;
        }
        for (String line : beforeLines) {
            if (!isImportLine(line) && !isIgnorableLine(line)) {
                return false;
            }
        }
        for (String line : afterLines) {
            if (!isImportLine(line) && !isIgnorableLine(line)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isImportLine(@NotNull String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("import ");
    }

    private static boolean isIgnorableLine(@NotNull String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty();
    }

    @NotNull
    private static String normalizeLine(@NotNull String line) {
        return line.replaceAll("\\s+", "");
    }

    @Nullable
    private static String resolveJavaSymbolContext(@Nullable VirtualFile virtualFile,
                                                   int preferredLine,
                                                   int fallbackLine) {
        if (virtualFile == null || !"java".equalsIgnoreCase(virtualFile.getExtension())) {
            return null;
        }
        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        if (project == null || project.isDisposed()) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        }
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        int lineCount = document.getLineCount();
        int line = preferredLine >= 0 && preferredLine < lineCount ? preferredLine : fallbackLine;
        if (line < 0 || line >= lineCount) {
            return null;
        }
        int offset = DocumentUtil.getLineStartOffset(line, document);
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null) {
            return null;
        }
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        PsiField field = PsiTreeUtil.getParentOfType(element, PsiField.class, false);

        String className = psiClass != null ? psiClass.getName() : null;
        if (method != null) {
            String methodSig = method.getName() + method.getParameterList().getText();
            return className != null ? className + "#" + methodSig : methodSig;
        }
        if (field != null) {
            return className != null ? className + "#" + field.getName() : field.getName();
        }
        return Objects.toString(className, null);
    }
}
