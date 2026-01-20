package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 注入问题用的 IntentionAction.
 */
public class AIViolationIntention implements IntentionAction {
    private final CodeViolation violation;

    public AIViolationIntention(@NotNull CodeViolation violation) {
        this.violation = violation;
    }

    @NotNull
    @Override
    public String getText() {
        return RepairerBundle.message("fix.ai.generic.name");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RepairerBundle.message("fix.ai.generic.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file != null && editor != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        Document document = editor.getDocument();
        TextRange range = computeRange(document, violation);
        if (range == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitDocument(document);
        ViolationFixer.apply(project, file, document, range, violation);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
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
