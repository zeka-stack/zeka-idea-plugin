package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 基于通用 Violation 的 AI QuickFix.
 */
public class AIViolationFix implements LocalQuickFix {
    private final CodeViolation violation;

    public AIViolationFix(@NotNull CodeViolation violation) {
        this.violation = violation;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.generic.family");
    }

    @NotNull
    @Override
    public String getName() {
        return RepairerBundle.message("fix.ai.generic.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element == null) {
            return;
        }
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }
        TextRange range = computeRange(document, violation);
        if (range == null) {
            return;
        }
        ViolationFixer.apply(project, file, document, range, violation);
    }

    private TextRange computeRange(@NotNull Document document, @NotNull CodeViolation v) {
        int line = v.startLine > 0 ? v.startLine : 1;
        if (line > document.getLineCount()) {
            return null;
        }
        int startLine = line - 1;
        int startOffset = document.getLineStartOffset(startLine);
        int endLine = v.endLine > 0 ? v.endLine - 1 : startLine;
        if (endLine >= document.getLineCount()) {
            endLine = document.getLineCount() - 1;
        }
        int endOffset = document.getLineEndOffset(endLine);
        return new TextRange(startOffset, endOffset);
    }
}
