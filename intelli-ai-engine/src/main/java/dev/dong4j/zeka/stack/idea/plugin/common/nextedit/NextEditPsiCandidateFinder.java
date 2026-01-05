package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 提供用于查找下一个编辑候选项的功能
 * <p> 此类用于在给定的项目和编辑器环境中, 根据指定的编辑记录和限制条件, 查找符合条件的编辑候选项
 * <p> 该类主要用于处理文本编辑时的上下文匹配, 确保在正确的范围内查找相似的文本片段
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditPsiCandidateFinder {
    /**
     * 查找下一个编辑候选项
     * <p> 根据给定的项目, 编辑器和编辑记录, 查找符合条件的编辑候选项列表
     * <p> 如果旧文本为空白或包含换行符, 则返回空列表
     * <p> 如果文档未提交, 则返回空列表
     * <p> 如果无法获取 PSI 文件, 则返回空列表
     * <p> 在 PSI 文件中搜索与旧文本匹配的元素, 并返回符合条件的候选项
     *
     * @param project 当前项目
     * @param editor  当前编辑器
     * @param edit    编辑记录, 包含旧文本和编辑范围
     * @param limit   返回的最大候选项数量
     * @return 符合条件的编辑候选项列表
     */
    @NotNull
    List<NextEditCandidate> findCandidates(@NotNull Project project,
                                           @NotNull Editor editor,
                                           @NotNull NextEditRecord edit,
                                           int limit) {
        String oldText = edit.oldText();
        if (oldText.isBlank() || oldText.contains("\n")) {
            return List.of();
        }
        return ReadAction.compute(() -> {
            Document document = editor.getDocument();
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            if (!documentManager.isCommitted(document)) {
                return List.of();
            }
            PsiFile psiFile = documentManager.getPsiFile(document);
            if (psiFile == null) {
                return List.of();
            }
            PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(project);
            List<NextEditCandidate> candidates = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            short searchContext = (short) (UsageSearchContext.IN_CODE | UsageSearchContext.IN_STRINGS);
            searchHelper.processElementsWithWord((element, offsetInElement) -> {
                TextRange range = element.getTextRange();
                if (range == null) {
                    return true;
                }
                String elementText = element.getText();
                if (elementText == null || !elementText.equals(oldText)) {
                    return true;
                }
                int start = range.getStartOffset();
                int end = range.getEndOffset();
                if (overlaps(start, end, edit.startOffset(), edit.endOffset())) {
                    return true;
                }
                if (seen.add(start)) {
                    int line = document.getLineNumber(start);
                    String preview = previewAt(document.getText(), start);
                    candidates.add(new NextEditCandidate(start, end, line, 1.0, preview, "psi"));
                }
                return candidates.size() < limit;
            }, GlobalSearchScope.fileScope(project, psiFile.getVirtualFile()), oldText, searchContext, true);
            return candidates;
        });
    }

    /**
     * 判断两个范围是否重叠
     * <p> 给定两个范围 [start, end] 和 [lastStart, lastEnd], 判断它们是否重叠
     * <p> 重叠的条件是 start 小于 lastEnd 且 end 大于 lastStart
     *
     * @param start     第一个范围的起始位置
     * @param end       第一个范围的结束位置
     * @param lastStart 第二个范围的起始位置
     * @param lastEnd   第二个范围的结束位置
     * @return 如果两个范围重叠, 则返回 true; 否则返回 false
     */
    private boolean overlaps(int start, int end, int lastStart, int lastEnd) {
        return start < lastEnd && end > lastStart;
    }

    /**
     * 获取指定偏移量所在行的预览文本
     * <p> 从给定的文本中提取指定偏移量所在的行, 并将其替换换行符为 "\\n" 返回
     * <p> 如果提取的文本长度超过 120 个字符, 则截断为前 120 个字符
     *
     * @param text   原始文本
     * @param offset 偏移量, 表示要获取的行的起始位置
     * @return 指定偏移量所在行的预览文本, 长度不超过 120 个字符, 换行符被替换为 "\\n"
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
}
