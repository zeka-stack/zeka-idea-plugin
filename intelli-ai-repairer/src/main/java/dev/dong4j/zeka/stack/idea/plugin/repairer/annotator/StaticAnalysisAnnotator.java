package dev.dong4j.zeka.stack.idea.plugin.repairer.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.AIViolationIntention;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 将外部静态分析结果注入到 Problems/编辑器高亮.
 */
public class StaticAnalysisAnnotator extends ExternalAnnotator<Project, List<CodeViolation>> {
    @Override
    public @Nullable Project collectInformation(@NotNull PsiFile file) {
        return file.getProject();
    }

    @Override
    public @Nullable List<CodeViolation> doAnnotate(Project project) {
        if (project == null) {
            return null;
        }
        return ViolationCache.getInstance(project).getAll();
    }

    @Override
    public void apply(@NotNull PsiFile file,
                      List<CodeViolation> violations,
                      @NotNull AnnotationHolder holder) {
        if (violations == null || violations.isEmpty()) {
            return;
        }
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : null;
        if (filePath == null) {
            return;
        }
        for (CodeViolation v : violations) {
            if (!filePath.equals(v.filePath)) {
                continue;
            }
            TextRange range = computeRange(file, v);
            if (range == null) {
                continue;
            }
            holder.newAnnotation(HighlightSeverity.WARNING, v.message)
                .range(range)
                .withFix(new AIViolationIntention(v))
                .create();
        }
    }

    private TextRange computeRange(PsiFile file, CodeViolation v) {
        if (v.startLine <= 0 || v.startLine > file.getViewProvider().getDocument().getLineCount()) {
            return null;
        }
        int startLine = v.startLine - 1;
        int startOffset = file.getViewProvider().getDocument().getLineStartOffset(startLine);
        int endLine = v.endLine > 0 ? v.endLine - 1 : startLine;
        int endOffset = file.getViewProvider().getDocument().getLineEndOffset(endLine);
        return new TextRange(startOffset, endOffset);
    }
}
