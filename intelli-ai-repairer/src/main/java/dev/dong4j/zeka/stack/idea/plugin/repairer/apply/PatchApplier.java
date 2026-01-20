package dev.dong4j.zeka.stack.idea.plugin.repairer.apply;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;

/**
 * 安全应用修复片段.
 */
public final class PatchApplier {
    private PatchApplier() {
    }

    public static void apply(Project project, PsiFile file, TextRange range, String original, String replacement) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }
        String current = document.getText(range);
        if (!current.equals(original)) {
            NotificationUtil.showWarning(project, RepairerBundle.message("error.code.changed"));
            return;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(range.getStartOffset(), range.getEndOffset(), replacement);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }
}
