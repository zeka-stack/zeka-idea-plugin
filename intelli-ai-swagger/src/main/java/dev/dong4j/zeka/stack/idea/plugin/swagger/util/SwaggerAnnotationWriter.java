package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;

/**
 * Swagger 注解写回工具
 */
public class SwaggerAnnotationWriter {

    private final Project project;
    private final SettingsState settings;

    public SwaggerAnnotationWriter(@NotNull Project project, @NotNull SettingsState settings) {
        this.project = project;
        this.settings = settings;
    }

    public void insertAnnotations(@NotNull PsiMethod method, @NotNull String rawAnnotationText) {
        ApplicationManager.getApplication().invokeLater(() -> CommandProcessor.getInstance().executeCommand(
            project,
            () -> ApplicationManager.getApplication().runWriteAction(() -> doInsert(method, rawAnnotationText)),
            "Insert Swagger Annotations",
            PluginContents.PLUGIN_NAME
                                                                                                           ));
    }

    private void doInsert(@NotNull PsiMethod method, @NotNull String rawAnnotationText) {
        String cleaned = SwaggerAnnotationWriterUtil.cleanAnnotationText(rawAnnotationText);
        if (cleaned.isBlank()) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.swagger.empty"));
            return;
        }

        if (!settings.overrideExisting && SwaggerAnnotationUtil.hasSwaggerAnnotations(method)) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.swagger.exists"));
            return;
        }

        SwaggerAnnotationUtil.deleteSwaggerAnnotations(method);
        PsiModifierList modifierList = method.getModifierList();
        if (modifierList == null) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.no.document"));
            return;
        }
        String qualified = SwaggerAnnotationUtil.qualifySwaggerAnnotations(cleaned);

        Document document = FileDocumentManager.getInstance()
            .getDocument(method.getContainingFile().getVirtualFile());
        if (document == null) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.no.document"));
            return;
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);

        int insertOffset = modifierList.getTextRange().getStartOffset();
        int lineNumber = document.getLineNumber(insertOffset);
        int lineStart = document.getLineStartOffset(lineNumber);
        String indent = document.getText(new TextRange(lineStart, insertOffset));

        String indented = SwaggerAnnotationWriterUtil.indentLines(qualified, indent);
        document.insertString(lineStart, indented + "\n");
        PsiDocumentManager.getInstance(project).commitDocument(document);

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(method);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }
}
